package com.xigeandwillian.parkingsystem.common.utils;

import com.xigeandwillian.parkingsystem.client.vo.user.UserVO;

/**
 * @author willian
 * @description: ThreadLocal
 * @date 2021/12/27
 */
public class UserHolder {
    private static final ThreadLocal<Long> t = new ThreadLocal<>();

    public static void save(Long userId) {
        t.set(userId);
    }

    public static long get() {
        return t.get();
    }

    public static void remove() {
        t.remove();
    }
}
