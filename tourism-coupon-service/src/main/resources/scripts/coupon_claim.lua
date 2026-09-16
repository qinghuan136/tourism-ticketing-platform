-- KEYS[1] coupon:activity:{activityId}:enabled
-- KEYS[2] coupon:activity:{activityId}:meta
-- KEYS[3] coupon:activity:{activityId}:stock
-- KEYS[4] coupon:activity:{activityId}:claimed-users
-- KEYS[5] coupon:activity:{activityId}:user-request:{userId}
-- KEYS[6] coupon:activity:{activityId}:claim-outbox
--
-- ARGV[1] userId
-- ARGV[2] requestId
-- ARGV[3] nowEpochMillis
-- ARGV[4] 可重建 CouponClaimCommand 的 Outbox 内容

-- 重复请求优先返回原 requestId。
-- 即使用户因为网络问题重复点击，也不会再次扣减库存。
local existingRequestId = redis.call('GET', KEYS[5])
if existingRequestId then
    return 1
end

-- enabled 只有在预热全部完成后才会被设置为 1。
local enabled = redis.call('GET', KEYS[1])
if enabled ~= '1' then
    return 2
end

-- 从预热生成的 Hash 中读取活动状态和领取时间。
local metadata = redis.call(
    'HMGET',
    KEYS[2],
    'status',
    'claimStartAt',
    'claimEndAt'
)

if metadata[1] ~= 'PUBLISHED' then
    return 4
end

local now = tonumber(ARGV[3])
local claimStartAt = tonumber(metadata[2])
local claimEndAt = tonumber(metadata[3])

if now < claimStartAt then
    return 3
end

if now >= claimEndAt then
    return 4
end

local stock = tonumber(redis.call('GET', KEYS[3]))

if stock <= 0 then
    return 5
end

-- Lua 脚本执行期间不会被其他 Redis 命令插入，
-- 所以库存检查和库存扣减是一个原子操作。
redis.call('DECR', KEYS[3])

-- 记录该游客已经取得当前活动的抢券资格。
redis.call('SADD', KEYS[4], ARGV[1])
redis.call('SET', KEYS[5], ARGV[2])

-- 先把待发送消息与资格、库存一起原子登记。
-- 即使应用在调用 Kafka 前宕机，定时任务也能从这里恢复消息。
redis.call('ZADD', KEYS[6], now, ARGV[4])

-- 防重数据与活动缓存保持相同的过期时间。
local activityTtl = redis.call('PTTL', KEYS[1])
redis.call('PEXPIRE', KEYS[4], activityTtl)
redis.call('PEXPIRE', KEYS[5], activityTtl)
redis.call('PEXPIRE', KEYS[6], activityTtl)

return 0
