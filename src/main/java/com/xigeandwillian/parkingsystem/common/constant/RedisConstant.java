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
         * 注册手机号标记TTL(分钟)
         */
        public static final long USER_REGISTER_PHONE_TTL_MIN = 10;
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



    public static class Coupon {
        /** Redis key: 可领取优惠券缓存 */
        public static final String AVAILABLE_KEY = "coupon:available";
        /** 可领取优惠券缓存TTL(分钟) */
        public static final long AVAILABLE_TTL_MIN = 10;
        /** Redis key: 秒杀优惠券详情缓存 */
        public static final String DETAIL_KEY = "coupon:detail:map";
        /** 秒杀优惠券详情缓存TTL(分钟) */
        public static final long DETAIL_TTL_MIN = 5;
        /** Redis key: 秒杀优惠券实时库存 */
        public static final String SECKILL_STOCK = "coupon:seckill:stock:";
        /** Redis key: 用户已领取优惠券标记(Set结构) */
        public static final String BOUGHT_KEY = "coupon:bought:";
        /** Redis key: 优惠券静态详情缓存 */
        public static final String STATIC_KEY = "coupon:static:";
        /** 优惠券静态详情缓存TTL(分钟) */
        public static final long STATIC_TTL_MIN = 2;
        /** 优惠券静态详情空值缓存TTL(分钟) */
        public static final long STATIC_NULL_TTL_MIN = 5;

        /** 秒杀库存Redis缓存TTL(天) */
        public static final long SECKILL_STOCK_TTL_DAY = 30;
        /** 可领取优惠券重建分布式锁 */
        public static final String COUPON_REBUILD_LOCK_AVAILABLE = "coupon:rebuild:lock:available";
        /** 优惠券详情重建分布式锁 */
        public static final String COUPON_REBUILD_LOCK_DETAIL = "coupon:rebuild:lock:detail";
    }

    public static class Cache {
        /** Redis分布式锁默认TTL(分钟) */
        public static final long LOCK_TTL_MIN = 1;
        /** Redis 分布式锁前缀 */
        public static final String LOCK_PREFIX = "lock:";
    }

    public static class Order {
        /** 订单号自增计数前缀 */
        public static final String ORDER_ID_PREFIX = "OrderId:";
    }

    public static class Parking {
        /** 停车场缓存信息*/
        public static final String PARKING_LOT_INFO = "parking:lot:info:";
        /** 停车场坐标信息*/
        public static final String PARKING_GEO = "parking:geo";
        /** 车位状态Bitmap缓存前缀 */
        public static final String PARKING_SPOT_STATUS = "parking:spot:status:";
        /** 车位列表缓存前缀 */
        public static final String PARKING_SPOT_LIST = "parking:spot:list:";
        /** 车位列表缓存TTL(分钟) */
        public static final long PARKING_SPOT_LIST_TTL_MIN = 10;
        /** 停车场可用车位数缓存(Hash结构) */
        public static final String PARKING_AVAILABLE_SPOTS = "parking:availableSpots";
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

        /** Redis异常数据库查取停车场数据默认量*/
        public static final long PARKING_DEFAULT_NUMBER = 20;
        /** 分页默认页码 */
        public static final int DEFAULT_PAGE = 1;
        /** 分页默认每页条数 */
        public static final int DEFAULT_PAGE_SIZE = 15;
        /** 仪表盘最近订单查询条数 */
        public static final int DASHBOARD_RECENT_ORDER_LIMIT = 10;
        /** 停车场全量列表缓存 */
        public static final String PARKING_LOT_LIST_ALL = "parking:lot:list:all";
        /** 停车场全量列表缓存TTL(分钟) */
        public static final long PARKING_LOT_LIST_ALL_TTL_MIN = 30;
        /** 停车场信息缓存TTL(天) */
        public static final long PARKING_LOT_INFO_TTL_DAY = 7;
        /** Redis通知频道 */
        public static final String NOTIFICATION_USER_CHANNEL = "notification:user";
        /** 仪表盘今日订单数缓存 */
        public static final String DASHBOARD_TODAY_ORDERS = "dashboard:today:orders";
        /** 仪表盘今日收入缓存 */
        public static final String DASHBOARD_TODAY_REVENUE = "dashboard:today:revenue";
    }
}
