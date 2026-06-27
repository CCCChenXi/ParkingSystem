package com.xigeandwillian.parkingsystem.common.service.impl;

import com.xigeandwillian.parkingsystem.common.service.service.RedisService;
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
    public Boolean hasKey(String key) {
        try {
            return stringRedisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis操作失败: hasKey, key={}", key, e);
            return null;
        }
    }

    @Override
    public String get(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis操作失败: get, key={}", key, e);
            return null;
        }
    }

    @Override
    public Boolean set(String key, String value, long ttl, TimeUnit unit) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttl, unit);
            return true;
        } catch (Exception e) {
            log.error("Redis操作失败: set, key={}", key, e);
            return null;
        }
    }

    @Override
    public Boolean delete(String key) {
        try {
            return stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis操作失败: delete, key={}", key, e);
            return null;
        }
    }

    @Override
    public Long increment(String key) {
        try {
            return stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis操作失败: increment, key={}", key, e);
            return null;
        }
    }

    @Override
    public Boolean expire(String key, long ttl, TimeUnit unit) {
        try {
            return stringRedisTemplate.expire(key, ttl, unit);
        } catch (Exception e) {
            log.error("Redis操作失败: expire, key={}", key, e);
            return null;
        }
    }
}
