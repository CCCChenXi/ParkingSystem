package com.xigeandwillian.parkingsystem.common.utils;

/**
 * @author willian
 * @description: ThreadLocal
 */
public class UserHolder {
    private static final ThreadLocal<Long> t = new ThreadLocal<>();

    public static void save(Long userId) {
        t.set(userId);
    }

    public static Long get() {
        return t.get();
    }

    public static void remove() {
        t.remove();
    }
}
