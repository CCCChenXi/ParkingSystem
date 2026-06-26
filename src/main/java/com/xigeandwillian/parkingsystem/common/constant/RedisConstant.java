package com.xigeandwillian.parkingsystem.common.constant;

/**
 * @author xige
 */
public class RedisConstant {
    public static final String USER_PHONE_CODE = "user:phone:";
    public static final long USER_CODE_TTL = 300000;
    public static final String REGISTER_PHONE="register:phone:";
    public static final long REGISTER_PHONE_TTL = 864000000;
    public static final int LOGIN_ERROR_LIMIT=5;
    public static final String ADMIN_LOGIN_COUNT="admin:login:count:";
    public static final long LOGIN_ERROR_TTL=300000;
    public static final String ADMIN_ERROR_LOCK="admin:error:lock:";
    public static final long LOGIN_ERROR_RESET_TTL = 86400000;


    public static final String USER_LOGIN_COUNT = "login:account:";
    public static final String USER_EDIT_TIMES = "user:edit:";

    public static final String USER_VO = "user:vo:";
    public static final long USER_VO_TTL = 30;
}
