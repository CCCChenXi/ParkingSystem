package com.xigeandwillian.parkingsystem.common.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class RedisLock implements Lock {

    //成员变量
    private String lockType;
    private StringRedisTemplate stringRedisTemplate;

    //构造方法
    public RedisLock(String lockType, StringRedisTemplate stringRedisTemplate) {
        this.lockType = lockType;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //锁前缀
    private static final String lockPredix = "lock:";
    //锁标识(值)前缀
    private static final String lockValPrefix = UUID.randomUUID().toString(true) + "-";
    //lua脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }



    @Override
    public boolean tryLock(Long LockTTL) {
        String unique = lockValPrefix + Thread.currentThread().getName();
        Boolean lockResult = stringRedisTemplate
                .opsForValue().setIfAbsent(lockPredix + lockType, unique, LockTTL, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(lockResult);
    }

    @Override
    public void releaseLock() {
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(lockPredix + lockType),
                lockValPrefix + Thread.currentThread().getName());
    }
}
