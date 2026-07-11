package com.xigeandwillian.parkingsystem.common.service;

import com.xigeandwillian.parkingsystem.common.result.CacheResult;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    CacheResult<Boolean> hasKey(String key);

    CacheResult<String> get(String key);

    CacheResult<Boolean> set(String key, String value, long ttl, TimeUnit unit);

    CacheResult<Boolean> delete(String key);

    CacheResult<Long> increment(String key);

    CacheResult<Boolean> expire(String key, long ttl, TimeUnit unit);
}
