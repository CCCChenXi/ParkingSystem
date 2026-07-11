package com.xigeandwillian.parkingsystem.common.constant;

public class MQConstant {

    /** 预约交换机 */
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    /** 预约通知队列 */
    public static final String BOOKING_NOTIFY_QUEUE = "booking.notify.queue";
    /** 预约通知路由键 */
    public static final String BOOKING_NOTIFY_ROUTING_KEY = "booking.notify";
    /** 预约延迟队列 */
    public static final String BOOKING_DELAY_QUEUE = "booking.delay.queue";
    /** 预约延迟路由键 */
    public static final String BOOKING_DELAY_ROUTING_KEY = "booking.delay";
    /** 预约过期队列 */
    public static final String BOOKING_EXPIRE_QUEUE = "booking.expire.queue";
    /** 预约过期路由键 */
    public static final String BOOKING_EXPIRE_ROUTING_KEY = "booking.expire";

    /** 缓存失效广播交换机(Fanout) */
    public static final String CACHE_INVALIDATE_EXCHANGE = "cache.invalidate.exchange";

    /** 秒杀订单队列 */
    public static final String SECKILL_QUEUE = "seckill.order.queue";
    /** 秒杀订单交换机 */
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";
    /** 秒杀订单路由键 */
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

    /** 车位缓存重试交换机(Direct) */
    public static final String PARKING_SPOT_CACHE_RETRY_EXCHANGE = "parking.spot.cache.retry.exchange";
    /** 车位缓存更新源队列(TTL 30s，超时进入重试) */
    public static final String PARKING_SPOT_CACHE_UPDATE_SOURCE_QUEUE = "parking.spot.cache.update.source.queue";
    /** 车位缓存创建源队列(TTL 10s，超时进入重试) */
    public static final String PARKING_SPOT_CACHE_CREATE_SOURCE_QUEUE = "parking.spot.cache.create.source.queue";
    /** 车位缓存重试处理队列 */
    public static final String PARKING_SPOT_CACHE_RETRY_PROC_QUEUE = "parking.spot.cache.retry.proc.queue";
    /** 车位缓存重试告警队列 */
    public static final String PARKING_SPOT_CACHE_RETRY_ALERT_QUEUE = "parking.spot.cache.retry.alert.queue";

    /** 车位缓存更新源队列TTL(毫秒) */
    public static final long PARKING_SPOT_CACHE_UPDATE_TTL_MS = 30000;
    /** 车位缓存创建源队列TTL(毫秒) */
    public static final long PARKING_SPOT_CACHE_CREATE_TTL_MS = 10000;

    // ──── 停车场缓存初始化重试 ────
    public static final String PARKING_LOT_CACHE_INIT_EXCHANGE = "parking.lot.cache.init.exchange";
    public static final String PARKING_LOT_CACHE_INIT_DELAY_QUEUE = "parking.lot.cache.init.delay.queue";
    public static final String PARKING_LOT_CACHE_INIT_PROC_QUEUE = "parking.lot.cache.init.proc.queue";
    public static final String PARKING_LOT_CACHE_INIT_ALERT_QUEUE = "parking.lot.cache.init.alert.queue";
    public static final long PARKING_LOT_CACHE_INIT_TTL_MS = 10000;

    // ──── 车位释放重试 ────
    public static final String SPOT_RELEASE_RETRY_EXCHANGE = "spot.release.retry.exchange";
    public static final String SPOT_RELEASE_RETRY_DELAY_QUEUE = "spot.release.retry.delay.queue";
    public static final String SPOT_RELEASE_RETRY_PROC_QUEUE = "spot.release.retry.proc.queue";
    public static final String SPOT_RELEASE_RETRY_ALERT_QUEUE = "spot.release.retry.alert.queue";
    public static final long SPOT_RELEASE_RETRY_TTL_MS = 10000;

    /** 重试处理队列路由键 */
    public static final String PROC_ROUTING_KEY = "proc";
    /** 默认最大重试次数 */
    public static final int DEFAULT_RETRY_MAX = 10;
    /** 告警队列路由键 */
    public static final String ALERT_ROUTING_KEY = "alert";
}
