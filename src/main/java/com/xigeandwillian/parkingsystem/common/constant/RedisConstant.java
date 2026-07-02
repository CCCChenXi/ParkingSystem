package com.xigeandwillian.parkingsystem.common.constant;

/**
 * @author xige
 */
public class RedisConstant {

    public static class Auth {
        /** 管理员登录错误次数上限 */
        public static final int ADMIN_LOGIN_ERROR_LIMIT = 5;
        /** Redis key: 管理员登录错误计数 */
        public static final String ADMIN_LOGIN_COUNT = "admin:login:count:";
        /** 登录锁定时间(分钟) */
        public static final long ADMIN_LOGIN_ERROR_TTL_MIN = 5;
        /** Redis key: 管理员登录锁定标记 */
        public static final String ADMIN_LOGIN_LOCK = "admin:login:lock:";
        /** 登录错误计数重置周期(天) */
        public static final long ADMIN_LOGIN_ERROR_RESET_TTL_DAY = 1;
        /** Redis key: 管理员会话缓存前缀 */
        public static final String ADMIN_SESSION_PREFIX = "admin:session:";
        /** 管理员会话TTL(小时) */
        public static final long ADMIN_SESSION_TTL_HOUR = 2;
    }

    public static class User {
        /** Redis key: 用户验证码 */
        public static final String USER_PHONE_CODE = "user:phone:code:";
        /** 验证码TTL(分钟) */
        public static final long USER_CODE_TTL_MIN = 5;
        /** Redis key: 用户注册手机号标记 */
        public static final String USER_REGISTER_PHONE = "user:register:phone:";
        /** 注册手机号标记TTL(天) */
        public static final long USER_REGISTER_PHONE_TTL_DAY = 1;
        /** Redis key: 用户登录错误计数 */
        public static final String USER_LOGIN_COUNT = "user:login:count:";
        /** 登录错误计数重置周期(天) */
        public static final long USER_LOGIN_COUNT_TTL_DAY = 1;
        /** 用户登录错误次数上限 */
        public static final int USER_LOGIN_ERROR_LIMIT = 5;
        /** Redis key: 用户登录锁定标记 */
        public static final String USER_LOGIN_LOCK = "user:login:lock:";
        /** 登录锁定时间(分钟) */
        public static final long USER_LOGIN_LOCK_TTL_MIN = 5;
        /** Redis key: 用户当月修改资料次数 */
        public static final String USER_EDIT_TIMES = "user:edit:times:";
        /** 每月修改次数上限 */
        public static final int USER_EDIT_LIMIT = 2;
        /** 每月修改次数重置周期(天) */
        public static final long USER_EDIT_TIMES_TTL_DAY = 30;
        /** Redis key: 用户会话缓存前缀 */
        public static final String USER_SESSION_PREFIX = "user:session:";
        /** 用户会话TTL(小时) */
        public static final long USER_SESSION_TTL_HOUR = 12;
    }

    public static class Parking {
        /** 停车场缓存信息*/
        public static final String PARKING_LOT_INFO = "parking:lot:info:";
        /** 停车场坐标信息*/
        public static final String PARKING_GEO = "parking:geo";
        public static final String PARKING_SPOT_STATUS = "parking:spot:status:";
        public static final String PARKING_LOT_AVAILABLE = "parking:lot:available:";
        /** 车位空闲状态值 */
        public static final String SPOT_STATUS_FREE = "0";
        /** 车位占用状态值 */
        public static final String SPOT_STATUS_OCCUPIED = "1";
        /** 车位状态临时表后缀 */
        public static final String SPOT_STATUS_TEMP_SUFFIX = "-temp";
        /** 可用车位重建分布式锁前缀 */
        public static final String PARKING_LOT_REBUILD_LOCK = "parking:lot:rebuild:available:";
        /** 停车场返回数量*/
        public static final long PARKING_RETURN_NUMBER = 10;
        /** 新建停车场默认车位数 */
        public static final int DEFAULT_TOTAL_SPOTS = 0;
        /** 关键字搜索最大长度 */
        public static final int KEYWORD_MAX_LENGTH = 20;
        /** 仪表盘-停车场总数缓存 */
        public static final String DASHBOARD_LOT_COUNT = "dashboard:lot:count";
        /** 仪表盘-车位总数缓存 */
        public static final String DASHBOARD_SPOT_COUNT = "dashboard:spot:count";
        /** 仪表盘-趋势缓存前缀 */
        public static final String DASHBOARD_TREND = "dashboard:trend:";
        /** 仪表盘趋势缓存TTL(天) */
        public static final long DASHBOARD_TREND_TTL_DAY = 7;
        /** 仪表盘计数缓存TTL(天) */
        public static final long DASHBOARD_COUNT_TTL_DAY = 1;
    }
}
