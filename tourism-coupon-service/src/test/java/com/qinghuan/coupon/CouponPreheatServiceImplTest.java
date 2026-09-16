package com.qinghuan.coupon;

import com.qinghuan.common.constant.cacheKeys.CouponConstant;
import com.qinghuan.pojo.entity.CouponActivity;
import com.qinghuan.pojo.enums.CouponActivityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponPreheatServiceImplTest {

    @Mock
    private CouponMapper couponMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private CouponPreheatServiceImpl preheatService;

    @BeforeEach
    void setUp() {
        preheatService = new CouponPreheatServiceImpl(
                couponMapper, stringRedisTemplate, redissonClient);
    }

    @Test
    void shouldResetDatabaseFlagWhenRedisCannotBeEnabled() {
        CouponActivity activity = new CouponActivity();
        activity.setId(1L);
        activity.setRemainingStock(10);
        activity.setStatus(CouponActivityStatus.PUBLISHED);
        activity.setClaimStartAt(LocalDateTime.now().plusMinutes(1));
        activity.setClaimEndAt(LocalDateTime.now().plusHours(1));

        when(couponMapper.listActivitiesToPreheat(any(), any()))
                .thenReturn(List.of(activity));
        when(couponMapper.findActivityForClaim(1L)).thenReturn(activity);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(couponMapper.markActivityCacheReady(eq(1L), any())).thenReturn(1);

        // 前面的缓存写入成功，但最后开放秒杀入口时 Redis 异常。
        doAnswer(invocation -> {
            if (CouponConstant.activityEnabledKey(1L).equals(invocation.getArgument(0))
                    && "1".equals(invocation.getArgument(1))) {
                throw new RuntimeException("Redis unavailable");
            }
            return null;
        }).when(valueOperations).set(
                anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS));

        assertEquals(0, preheatService.preheatUpcomingActivities());
        verify(couponMapper).resetActivityCacheReady(1L);
        verify(lock).unlock();
    }

    @Test
    void shouldSkipStalePreheatTaskWhenAnotherInstanceAlreadyFinished() {
        CouponActivity staleActivity = new CouponActivity();
        staleActivity.setId(1L);

        CouponActivity latestActivity = new CouponActivity();
        latestActivity.setId(1L);
        latestActivity.setCacheReady(true);

        when(couponMapper.listActivitiesToPreheat(any(), any()))
                .thenReturn(List.of(staleActivity));
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(couponMapper.findActivityForClaim(1L))
                .thenReturn(latestActivity);

        assertEquals(0, preheatService.preheatUpcomingActivities());

        // 已由其他实例完成预热时，不能再覆盖或删除它写入的 Redis 数据。
        verify(stringRedisTemplate, never()).opsForValue();
        verify(couponMapper, never()).markActivityCacheReady(anyLong(), any());
        verify(lock).unlock();
    }
}
