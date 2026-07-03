--KEYS[1] 临时状态表  KEYS[2]  正式状态表
--KEYS[3] 可用车位表  ARGV[1]  停车场id  ARGV[2]  可用车位数
if redis.call("EXISTS", KEYS[1]) == 1 then
    redis.call("RENAME", KEYS[1], KEYS[2]);
    redis.call("HSET", KEYS[3], ARGV[1], ARGV[2]);
    return 1;
else
    return 0;
end