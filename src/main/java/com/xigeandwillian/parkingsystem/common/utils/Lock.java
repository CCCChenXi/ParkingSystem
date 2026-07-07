package com.xigeandwillian.parkingsystem.common.utils;

public interface Lock {
    /**
     * 获取锁
     * @param LockTTL
     * @return
     */
    boolean tryLock(Long LockTTL);

    /**
     * 释放锁
     */
    void releaseLock();
}
