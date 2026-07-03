package com.xigeandwillian.parkingsystem.common.service.service;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    Boolean hasKey(String key);

    String get(String key);

    Boolean set(String key, String value, long ttl, TimeUnit unit);

    Boolean delete(String key);

    Long increment(String key);

    Boolean expire(String key, long ttl, TimeUnit unit);
}
