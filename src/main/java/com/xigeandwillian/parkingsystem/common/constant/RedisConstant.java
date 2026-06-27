package com.xigeandwillian.parkingsystem.common.constant;

/**
 * @author xige
 */
public class RedisConstant {

    /**
     * 登录注册模块常量
     */
    //手机验证码及有效期
    public static final String USER_PHONE_CODE = "user:phone:";
    public static final long USER_CODE_TTL = 300000;
    public static final String REGISTER_PHONE = "register:phone:";
    public static final long REGISTER_PHONE_TTL = 864000000;
    //登录错误次数
    public static final int LOGIN_ERROR_LIMIT = 5;
    public static final String ADMIN_LOGIN_COUNT = "admin:login:count:";
    //登录错误锁定
    public static final long LOGIN_ERROR_TTL = 300000;
    public static final String ADMIN_ERROR_LOCK = "admin:error:lock:";
    public static final long LOGIN_ERROR_RESET_TTL = 86400000;
    //登录次数
    public static final String USER_LOGIN_COUNT = "login:account:";
    //个人信息修改次数
    public static final String USER_EDIT_TIMES = "user:edit:";
    //用户信息
    public static final String USER_VO = "user:vo:";
    public static final long USER_VO_TTL = 30;


    /**
     * 停车场模块常量
     */
    //停车场信息
    public static final String PARKING_INFO = "parking:info:";
    public static final String PARKING_GEO = "parking:geo";
    public static final long PARKING_RETURN_NUMBER = 10;
    public static final long PARKING_DEFAULT_RADIUS = 100;
}
