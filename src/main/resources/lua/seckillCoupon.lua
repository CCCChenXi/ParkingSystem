if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return 2;
end
local stock = tonumber(redis.call('get', KEYS[1]))
if stock >= 1 then
    redis.call('decr', KEYS[1]);
    redis.call('sadd', KEYS[2], ARGV[1]);
    return 1;
end
return 0;