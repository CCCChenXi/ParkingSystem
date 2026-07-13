package com.xigeandwillian.parkingsystem.common.constant;

public class CaffeineConstant {

    /** 可领取优惠券本地缓存最大条数 */
    public static final long COUPON_MAXIMUM_SIZE = 500;
    /** 可领取优惠券本地缓存过期时间(分钟) */
    public static final long COUPON_EXPIRE_MINUTES = 3;

    /** 停车场信息本地缓存最大条数 */
    public static final long PARKING_MAXIMUM_SIZE = 1000;
    /** 停车场信息本地缓存过期时间(秒) */
    public static final long PARKING_EXPIRE_SECONDS = 60;

    /** 车位列表本地缓存最大条数 */
    public static final long PARKING_SPOT_MAXIMUM_SIZE = 500;
    /** 车位列表本地缓存过期时间(分钟) */
    public static final long PARKING_SPOT_EXPIRE_MINUTES = 5;

    /** 车位列表+状态组合缓存最大条数 */
    public static final long PARKING_SPOTS_MAXIMUM_SIZE = 500;
    /** 车位列表+状态组合缓存过期时间(秒) */
    public static final long PARKING_SPOTS_EXPIRE_SECONDS = 60;
    /** 车位列表+状态组合缓存Key前缀 */
    public static final String PARKING_SPOTS_KEY_PREFIX = "parkingSpots:";

    /** 车辆信息本地缓存最大条数 */
    public static final long VEHICLE_MAXIMUM_SIZE = 10000;
    /** 车辆信息本地缓存过期时间(分钟) */
    public static final long VEHICLE_EXPIRE_MINUTES = 1;

    /** 钱包余额本地缓存最大条数 */
    public static final long WALLET_MAXIMUM_SIZE = 10000;
    /** 钱包余额本地缓存过期时间(秒) */
    public static final long WALLET_EXPIRE_SECONDS = 30;

    /** 优惠券详情本地缓存最大条数 */
    public static final long COUPON_DETAIL_MAXIMUM_SIZE = 500;
    /** 优惠券详情本地缓存过期时间(分钟) */
    public static final long COUPON_DETAIL_EXPIRE_MINUTES = 5;

    /** 车辆信息本地缓存Key前缀 */
    public static final String VEHICLE_KEY = "vehicle:list:";
    /** 钱包余额本地缓存Key前缀 */
    public static final String WALLET_KEY = "wallet:balance:";
    /** 钱包流水本地缓存Key前缀 */
    public static final String WALLET_LOG_KEY = "wallet:log:";

    /** 停车场全量列表本地缓存最大条数 */
    public static final long PARKING_LOT_LIST_ALL_MAXIMUM_SIZE = 100;
    /** 停车场全量列表本地缓存过期时间(秒) */
    public static final long PARKING_LOT_LIST_ALL_EXPIRE_SECONDS = 300;
    /** 停车场全量列表本地缓存Key前缀 */
    public static final String PARKING_LOT_LIST_ALL_KEY = "parkingLot:list:all";

}
