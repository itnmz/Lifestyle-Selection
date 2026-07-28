



-- 因为判断锁和释放锁是两个动作，之间可能发生业务阻塞（阻塞时间超过TTL导致误删锁）
-- 为解决问题，使用了lua脚本来实现在判断是否释放锁与释放锁这两个动作的原子性

-- 比较线程标示与锁中的标示是否一致
if(redis.call('get', KEYS[1]) ==  ARGV[1]) then
    -- 释放锁 del key
    return redis.call('del', KEYS[1])
end
return 0