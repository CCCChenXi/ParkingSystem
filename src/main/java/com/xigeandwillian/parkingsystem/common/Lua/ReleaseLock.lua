-- 比较线程标识和锁中标识是否一致
if (redis.call('get', KEYS[1]) == ARGV[1])
-- 释放锁
then
    return redis.call('del', KEYS[1])
end
return 0