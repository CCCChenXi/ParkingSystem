package com.xigeandwillian.parkingsystem.common.utils;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.xigeandwillian.parkingsystem.common.constant.RedisConstant.Cache.LOCK_TTL;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void setWithTtl(String key, Object value, Long ttl) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), ttl, TimeUnit.SECONDS);
    }

    //逻辑过期
    public void setWithLogicalExpire(String key, Object value, Long ttl) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(ttl));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public Boolean tryLock(String key) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL, TimeUnit.SECONDS);
    }

    public void unLock(String key) {
        stringRedisTemplate.delete(key);
    }


}