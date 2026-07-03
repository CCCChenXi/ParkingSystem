package com.xigeandwillian.parkingsystem.common.constant;

/**
 * @author xige
 */
public class RedisConstant {

    public static class Admin {
        /**
         * 管理员登录错误次数上限
         */
        public static final int ADMIN_LOGIN_ERROR_LIMIT = 5;
        /**
         * Redis key: 管理员登录错误计数
         */
        public static final String ADMIN_LOGIN_COUNT = "admin:login:count:";
        /**
         * 登录锁定时间(分钟)
         */
        public static final long ADMIN_LOGIN_ERROR_TTL_MIN = 5;
        /**
         * Redis key: 管理员登录锁定标记
         */
        public static final String ADMIN_LOGIN_LOCK = "admin:login:lock:";
        /**
         * 登录错误计数重置周期(天)
         */
        public static final long ADMIN_LOGIN_ERROR_RESET_TTL_DAY = 1;
        /**
         * Redis key: 管理员会话缓存前缀
         */
        public static final String ADMIN_SESSION_PREFIX = "admin:session:";
        /**
         * 管理员会话TTL(小时)
         */
        public static final long ADMIN_SESSION_TTL_HOUR = 2;
    }

    public static class User {
        /**
         * Redis key: 用户验证码
         */
        public static final String USER_PHONE_CODE = "user:phone:code:";
        /**
         * 验证码TTL(分钟)
         */
        public static final long USER_CODE_TTL_MIN = 5;
        /**
         * Redis key: 用户注册手机号标记
         */
        public static final String USER_REGISTER_PHONE = "user:register:phone:";
        /**
         * 注册手机号标记TTL(天)
         */
        public static final long USER_REGISTER_PHONE_TTL_DAY = 1;
        /**
         * Redis key: 用户登录错误计数
         */
        public static final String USER_LOGIN_COUNT = "user:login:count:";
        /**
         * 登录错误计数重置周期(天)
         */
        public static final long USER_LOGIN_COUNT_TTL_DAY = 1;
        /**
         * 用户登录错误次数上限
         */
        public static final int USER_LOGIN_ERROR_LIMIT = 5;
        /**
         * Redis key: 用户登录锁定标记
         */
        public static final String USER_LOGIN_LOCK = "user:login:lock:";
        /**
         * 登录锁定时间(分钟)
         */
        public static final long USER_LOGIN_LOCK_TTL_MIN = 5;
        /**
         * Redis key: 用户当月修改资料次数
         */
        public static final String USER_EDIT_TIMES = "user:edit:times:";
        /**
         * 每月修改次数上限
         */
        public static final int USER_EDIT_LIMIT = 2;
        /**
         * 每月修改次数重置周期(天)
         */
        public static final long USER_EDIT_TIMES_TTL_DAY = 30;
        /**
         * Redis key: 用户会话缓存前缀
         */
        public static final String USER_SESSION_PREFIX = "user:session:";
        /**
         * 用户会话TTL(小时)
         */
        public static final long USER_SESSION_TTL_HOUR = 12;
    }

    public static class Parking {
        /*停车场缓存信息(不包含可用车位信息)*/
        public static final String PARKING_INFO = "parking:info:";
        /*停车场可用车位*/
        public static final String PARKING_AVAILABLE_SPOTS = "parking:availableSpots";
        /*停车场坐标信息*/
        public static final String PARKING_GEO = "parking:geo";
        /*停车场返回数量*/
        public static final long PARKING_RETURN_NUMBER = 10;
        /*Redis异常数据库查取停车场数据默认量*/
        public static final long PARKING_DEFAULT_NUMBER = 20;
    }

    public static final class Spots {
        /*停车场车位*/
        public static final String LOT_SPOTS = "parking:spot:";//+Lot_id
        /*停车场车位状态Bitmap*/
        public static final String LOT_SPOTS_BITMAP = "parking:spot:bitmap:";
    }

    public static class Cache {
        /*锁TTL->秒*/
        public static final long LOCK_TTL = 5L;
        /*锁key*/
        public static final String LOCK_KEY = "lock:";
        /*常态资源->天*/
        public static final long NORMAL_RESOURCE_TTL = 30L;
        /*空值TTL->分钟*/
        public static final long NULL_TTL = 5L;
    }

}
