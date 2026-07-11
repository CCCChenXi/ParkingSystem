package com.xigeandwillian.parkingsystem.common.constant;

public class CouponConstant {

    /** 优惠券类型：普通 */
    public static final int COUPON_TYPE_NORMAL = 0;
    /** 优惠券类型：秒杀 */
    public static final int COUPON_TYPE_SECKILL = 1;

    /** 用户优惠券状态：未使用 */
    public static final int USER_COUPON_STATUS_UNUSED = 0;
    /** 用户优惠券状态：已使用 */
    public static final int USER_COUPON_STATUS_USED = 1;

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 秒杀结果：已售罄 */
    public static final long SECKILL_RESULT_SOLD_OUT = 0;
    /** 秒杀结果：已领取 */
    public static final long SECKILL_RESULT_ALREADY_CLAIMED = 2;
}
