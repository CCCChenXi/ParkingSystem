package com.xigeandwillian.parkingsystem.common.utils;

public class AdminHolder {
    private static final ThreadLocal<Long> t = new ThreadLocal<>();

    public static void save(Long adminId) {
        t.set(adminId);
    }

    public static Long get() {
        return t.get();
    }

    public static void remove() {
        t.remove();
    }
}
