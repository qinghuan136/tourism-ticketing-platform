-- 消息发送失败或消费最终失败时，撤销 Redis 抢券资格。
--
-- KEYS[1] coupon:activity:{activityId}:user-request:{userId}
-- KEYS[2] coupon:activity:{activityId}:stock
-- KEYS[3] coupon:activity:{activityId}:claimed-users
--
-- ARGV[1] requestId
-- ARGV[2] userId

local currentRequestId = redis.call('GET', KEYS[1])

-- 只有当前占位仍然属于本次请求时才能补偿。
--
-- 如果 Key 已被删除，说明补偿已经执行过；
-- 如果值属于其他 requestId，也不能删除其他请求的数据。
if currentRequestId ~= ARGV[1] then
    return 0
end

-- 恢复本次 Lua 预扣的库存。
redis.call('INCR', KEYS[2])

-- 删除该游客的请求映射，使游客可以重新抢券。
redis.call('DEL', KEYS[1])

-- 从一人一单集合中删除该游客。
redis.call('SREM', KEYS[3], ARGV[2])

return 1
