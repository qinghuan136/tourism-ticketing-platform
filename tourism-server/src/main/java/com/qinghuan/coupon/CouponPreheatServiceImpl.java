package com.qinghuan.coupon;

import com.qinghuan.common.constant.cacheKeys.CouponConstant;
import com.qinghuan.common.constant.cacheKeys.LockConstant;
import com.qinghuan.coupon.model.CouponActivityCacheMetadata;
import com.qinghuan.pojo.entity.CouponActivity;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 优惠券活动预热实现。
 *
 * 预热横跨 MySQL 和 Redis，普通 @Transactional 无法让两者组成原子事务，
 * 因此这里不添加 @Transactional，而是通过执行顺序和失败回退保证可重试。
 */
@Slf4j
@Service
public class CouponPreheatServiceImpl implements CouponPreheatService {

    /** 项目业务时间统一使用中国时区。 */
    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Shanghai");

    private final CouponMapper couponMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    public CouponPreheatServiceImpl(
            CouponMapper couponMapper,
            StringRedisTemplate stringRedisTemplate,
            RedissonClient redissonClient) {
        this.couponMapper = couponMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
    }

    @Override
    public int preheatUpcomingActivities() {
        /*
         * 同一轮扫描只生成一次 now，保证数据库查询窗口使用同一个时间基准。
         */
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plus(CouponConstant.window);

        List<CouponActivity> activities =
                couponMapper.listActivitiesToPreheat(
                        now,
                        deadline
                );

        int successCount = 0;

        for (CouponActivity activity : activities) {
            try {
                if (preheatWithLock(activity)) {
                    successCount++;
                }
            } catch (Exception exception) {
                /*
                 * 单个活动失败不能阻断整批任务。
                 * cache_ready 仍为 false，下轮任务会继续重试。
                 */
                log.error(
                        "优惠券活动预热失败，activityId={}",
                        activity.getId(),
                        exception
                );
            }
        }

        return successCount;
    }

    /**
     * 使用活动级分布式锁执行预热。
     *
     * tryLock 不等待：
     * 如果其他实例正在预热当前活动，本实例直接跳过即可。
     */
    private boolean preheatWithLock(CouponActivity activity) {
        String lockKey = LockConstant.LOCK_COUPON_PREHEAT_PREFIX
                + activity.getId();

        RLock lock = redissonClient.getLock(lockKey);

        if (!lock.tryLock()) {
            return false;
        }

        try {
            /*
             * 多实例可能在加锁前同时查到 cache_ready=0。
             * 拿到锁后重新读取，避免后获得锁的实例用旧数据重复预热，
             * 并在条件更新失败时误删前一个实例已经写好的 Redis 缓存。
             */
            CouponActivity latestActivity =
                    couponMapper.findActivityForClaim(activity.getId());

            if (latestActivity == null
                    || Boolean.TRUE.equals(latestActivity.getCacheReady())) {
                return false;
            }

            return writeActivityCache(latestActivity);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 按安全顺序写入 Redis，并更新数据库预热标记。
     *
     * 顺序是：
     * 1. 先关闭入口；
     * 2. 写元数据和库存；
     * 3. 标记数据库预热完成；
     * 4. 最后启用 Redis 入口。
     */
    private boolean writeActivityCache(CouponActivity activity) {
        LocalDateTime now = LocalDateTime.now();


        Long activityId = activity.getId();

        String metadataKey =
                CouponConstant.activityMetadataKey(activityId);
        String stockKey =
                CouponConstant.activityStockKey(activityId);
        String enabledKey =
                CouponConstant.activityEnabledKey(activityId);

        /*
         * Redis 数据保留到活动结束后一段时间，
         * 方便处理延迟消息和短期结果查询。
         */
        long ttlSeconds = Duration.between(
                now,
                activity.getClaimEndAt()
        ).getSeconds() + CouponConstant.ACTIVITY_CACHE_GRACE_SECONDS;

        ttlSeconds = Math.max(ttlSeconds, 1L);

        CouponActivityCacheMetadata metadata =
                toCacheMetadata(activity);

        boolean databaseMarkedReady = false;

        try {
            /*
             * 先关闭入口。
             *
             * 即使上一次预热只完成了一半，本轮覆盖数据期间
             * Lua 也不会读取到不完整的活动数据。
             */
            stringRedisTemplate.opsForValue().set(
                    enabledKey,
                    "0",
                    ttlSeconds,
                    TimeUnit.SECONDS
            );

            /*
             * 库存必须使用 remainingStock。
             *
             * totalStock 是最初发行量，Redis 恢复时使用它会把
             * 已经发出的库存重新放出来。
             */
            stringRedisTemplate.opsForValue().set(
                    stockKey,
                    String.valueOf(activity.getRemainingStock()),
                    ttlSeconds,
                    TimeUnit.SECONDS
            );

            /*
             * 活动状态和领取时间使用 Hash 保存，
             * 后续 Lua 可以一次读取并进行时间比较。
             */
            stringRedisTemplate.opsForHash().putAll(
                    metadataKey,
                    metadata.toHash()
            );
            stringRedisTemplate.expire(
                    metadataKey,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );

            /*
             * Redis 必要数据都写入后，才更新 MySQL 预热标记。
             *
             * 这个条件更新同时检查活动是否仍然处于 PUBLISHED，
             * 防止活动在预热过程中已经被取消。
             */
            LocalDateTime preheatedAt = LocalDateTime.now();
            int updated = couponMapper.markActivityCacheReady(
                    activityId,
                    preheatedAt
            );

            if (updated == 0) {
                disableActivityCache(activityId);
                return false;
            }

            databaseMarkedReady = true;

            /*
             * enabled 必须最后写为 1。
             *
             * enabled=1 代表库存、元数据和数据库预热标记均已准备完成。
             * 活动尚未开始也没关系，Lua 还会检查 claimStartAt。
             */
            stringRedisTemplate.opsForValue().set(
                    enabledKey,
                    "1",
                    ttlSeconds,
                    TimeUnit.SECONDS
            );

            log.info(
                    "优惠券活动预热成功，activityId={}，remainingStock={}",
                    activityId,
                    activity.getRemainingStock()
            );

            return true;
        } catch (RuntimeException exception) {
            disableActivityCache(activityId);
            if (databaseMarkedReady) {
                // Redis 最终启用失败时允许下一轮定时任务重新预热该活动。
                couponMapper.resetActivityCacheReady(activityId);
            }
            throw exception;
        }
    }

    /**
     * 将数据库活动时间转换成 Lua 易于比较的毫秒时间戳。
     */
    private CouponActivityCacheMetadata toCacheMetadata(
            CouponActivity activity) {
        return new CouponActivityCacheMetadata(
                activity.getId(),
                activity.getStatus(),
                activity.getClaimStartAt()
                        .atZone(BUSINESS_ZONE)
                        .toInstant()
                        .toEpochMilli(),
                activity.getClaimEndAt()
                        .atZone(BUSINESS_ZONE)
                        .toInstant()
                        .toEpochMilli()
        );
    }


    @Override
    public void disableActivityCache(Long activityId) {
        try {
            /*
             * claimed-users 当前首次预热时可能还不存在，
             * Redis 删除不存在的 Key 也是安全的。
             *
             * user-request:{userId} 无法在不知道 userId 的情况下逐个删除，
             * 它们将在 TTL 到期后自动清理。
             */
            stringRedisTemplate.delete(List.of(
                    CouponConstant.activityMetadataKey(activityId),
                    CouponConstant.activityStockKey(activityId),
                    CouponConstant.activityEnabledKey(activityId),
                    CouponConstant.claimedUsersKey(activityId)
            ));
        } catch (Exception exception) {
            log.error(
                    "清理优惠券活动 Redis 数据失败，activityId={}",
                    activityId,
                    exception
            );
        }
    }
}
