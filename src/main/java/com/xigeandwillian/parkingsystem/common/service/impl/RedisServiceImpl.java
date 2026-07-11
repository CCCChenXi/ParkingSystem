package com.xigeandwillian.parkingsystem.common.service.impl;

import com.xigeandwillian.parkingsystem.common.result.CacheResult;
import com.xigeandwillian.parkingsystem.common.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CacheResult<Boolean> hasKey(String key) {
        try {
            return CacheResult.hit(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis操作失败: hasKey, key={}", key, e);
            return CacheResult.error();
        }
    }

    @Override
    public CacheResult<String> get(String key) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            return value != null ? CacheResult.hit(value) : CacheResult.miss();
        } catch (Exception e) {
            log.error("Redis操作失败: get, key={}", key, e);
            return CacheResult.error();
        }
    }

    @Override
    public CacheResult<Boolean> set(String key, String value, long ttl, TimeUnit unit) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttl, unit);
            return CacheResult.hit(true);
        } catch (Exception e) {
            log.error("Redis操作失败: set, key={}", key, e);
            return CacheResult.error();
        }
    }

    @Override
    public CacheResult<Boolean> delete(String key) {
        try {
            return CacheResult.hit(stringRedisTemplate.delete(key));
        } catch (Exception e) {
            log.error("Redis操作失败: delete, key={}", key, e);
            return CacheResult.error();
        }
    }

    @Override
    public CacheResult<Long> increment(String key) {
        try {
            return CacheResult.hit(stringRedisTemplate.opsForValue().increment(key));
        } catch (Exception e) {
            log.error("Redis操作失败: increment, key={}", key, e);
            return CacheResult.error();
        }
    }

    @Override
    public CacheResult<Boolean> expire(String key, long ttl, TimeUnit unit) {
        try {
            return CacheResult.hit(stringRedisTemplate.expire(key, ttl, unit));
        } catch (Exception e) {
            log.error("Redis操作失败: expire, key={}", key, e);
            return CacheResult.error();
        }
    }
}
