package com.xigeandwillian.parkingsystem.common.constant;

public class OrderConstant {
    /** 订单状态：已预约 */
    public static final int ORDER_STATUS_RESERVED = 0;
    /** 订单状态：进行中 */
    public static final int ORDER_STATUS_IN_PROGRESS = 1;
    /** 订单状态：已结算 */
    public static final int ORDER_STATUS_SETTLED = 2;
    /** 订单状态：已取消 */
    public static final int ORDER_STATUS_CANCELLED = 3;

    /** 预约订单TTL(分钟) */
    public static final long RESERVED_ORDER_TTL_MIN = 15;
    /** 每毫秒数 */
    public static final long MILLIS_PER_HOUR = 3600000;
    /** 时区偏移 */
    public static final String ZONE_OFFSET = "+8";

    /** 预约通知标题 */
    public static final String BOOK_TITLE = "预约成功";
    /** 入场通知标题 */
    public static final String ENTER_SUCCESS_TITLE = "入场成功";
    /** 结算通知标题 */
    public static final String SETTLE_TITLE = "结算成功";

    /** 预约通知内容 */
    public static final String BOOK_CONTENT = "恭喜您预约成功，请及时入场";
    /** 入场通知内容 */
    public static final String ENTER_SUCCESS_CONTENT = "恭喜您入场成功，请将车辆停放在对应车位上";
    /** 结算通知内容 */
    public static final String SETTLE_CONTENT = "结算成功，期待您的下次到来";
}
