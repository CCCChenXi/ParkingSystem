package com.xigeandwillian.parkingsystem.common.config;

import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public Queue seckillQueue() {
        return new Queue(MQConstant.SECKILL_QUEUE, true);
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(MQConstant.SECKILL_EXCHANGE);
    }

    @Bean
    public Binding seckillBinding(Queue seckillQueue, DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue).to(seckillExchange).with(MQConstant.SECKILL_ROUTING_KEY);
    }

    @Bean
    public FanoutExchange cacheInvalidateExchange() {
        return new FanoutExchange(MQConstant.CACHE_INVALIDATE_EXCHANGE);
    }

    @Bean
    public AnonymousQueue cacheInvalidateQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding cacheInvalidateBinding(AnonymousQueue cacheInvalidateQueue, FanoutExchange cacheInvalidateExchange) {
        return BindingBuilder.bind(cacheInvalidateQueue).to(cacheInvalidateExchange);
    }

    // ──── 停车场缓存初始化重试（保证最终成功） ────
    @Bean
    public DirectExchange parkingLotCacheInitExchange() {
        return new DirectExchange(MQConstant.PARKING_LOT_CACHE_INIT_EXCHANGE);
    }

    @Bean
    public Queue parkingLotCacheInitDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MQConstant.PARKING_LOT_CACHE_INIT_EXCHANGE);
        args.put("x-dead-letter-routing-key", MQConstant.PROC_ROUTING_KEY);
        args.put("x-message-ttl", MQConstant.PARKING_LOT_CACHE_INIT_TTL_MS);
        return new Queue(MQConstant.PARKING_LOT_CACHE_INIT_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue parkingLotCacheInitProcQueue() {
        return new Queue(MQConstant.PARKING_LOT_CACHE_INIT_PROC_QUEUE, true);
    }

    @Bean
    public Queue parkingLotCacheInitAlertQueue() {
        return new Queue(MQConstant.PARKING_LOT_CACHE_INIT_ALERT_QUEUE, true);
    }

    @Bean
    public Binding parkingLotCacheInitProcBinding(Queue parkingLotCacheInitProcQueue, DirectExchange parkingLotCacheInitExchange) {
        return BindingBuilder.bind(parkingLotCacheInitProcQueue).to(parkingLotCacheInitExchange).with(MQConstant.PROC_ROUTING_KEY);
    }

    @Bean
    public Binding parkingLotCacheInitAlertBinding(Queue parkingLotCacheInitAlertQueue, DirectExchange parkingLotCacheInitExchange) {
        return BindingBuilder.bind(parkingLotCacheInitAlertQueue).to(parkingLotCacheInitExchange).with(MQConstant.ALERT_ROUTING_KEY);
    }

    // ──── 车位释放重试（保证最终成功） ────
    @Bean
    public DirectExchange spotReleaseRetryExchange() {
        return new DirectExchange(MQConstant.SPOT_RELEASE_RETRY_EXCHANGE);
    }

    @Bean
    public Queue spotReleaseRetryDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MQConstant.SPOT_RELEASE_RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", MQConstant.PROC_ROUTING_KEY);
        args.put("x-message-ttl", MQConstant.SPOT_RELEASE_RETRY_TTL_MS);
        return new Queue(MQConstant.SPOT_RELEASE_RETRY_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue spotReleaseRetryProcQueue() {
        return new Queue(MQConstant.SPOT_RELEASE_RETRY_PROC_QUEUE, true);
    }

    @Bean
    public Queue spotReleaseRetryAlertQueue() {
        return new Queue(MQConstant.SPOT_RELEASE_RETRY_ALERT_QUEUE, true);
    }

    @Bean
    public Binding spotReleaseRetryProcBinding(Queue spotReleaseRetryProcQueue, DirectExchange spotReleaseRetryExchange) {
        return BindingBuilder.bind(spotReleaseRetryProcQueue).to(spotReleaseRetryExchange).with(MQConstant.PROC_ROUTING_KEY);
    }

    @Bean
    public Binding spotReleaseRetryAlertBinding(Queue spotReleaseRetryAlertQueue, DirectExchange spotReleaseRetryExchange) {
        return BindingBuilder.bind(spotReleaseRetryAlertQueue).to(spotReleaseRetryExchange).with(MQConstant.ALERT_ROUTING_KEY);
    }

    // ──── 车位缓存重试（保证最终成功） ────
    @Bean
    public DirectExchange parkingSpotCacheRetryExchange() {
        return new DirectExchange(MQConstant.PARKING_SPOT_CACHE_RETRY_EXCHANGE);
    }

    @Bean
    public Queue parkingSpotCacheUpdateSourceQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MQConstant.PARKING_SPOT_CACHE_RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", MQConstant.PROC_ROUTING_KEY);
        args.put("x-message-ttl", MQConstant.PARKING_SPOT_CACHE_UPDATE_TTL_MS);
        return new Queue(MQConstant.PARKING_SPOT_CACHE_UPDATE_SOURCE_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue parkingSpotCacheCreateSourceQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MQConstant.PARKING_SPOT_CACHE_RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", MQConstant.PROC_ROUTING_KEY);
        args.put("x-message-ttl", MQConstant.PARKING_SPOT_CACHE_CREATE_TTL_MS);
        return new Queue(MQConstant.PARKING_SPOT_CACHE_CREATE_SOURCE_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue parkingSpotCacheRetryProcQueue() {
        return new Queue(MQConstant.PARKING_SPOT_CACHE_RETRY_PROC_QUEUE, true);
    }

    @Bean
    public Binding parkingSpotCacheRetryProcBinding(Queue parkingSpotCacheRetryProcQueue, DirectExchange parkingSpotCacheRetryExchange) {
        return BindingBuilder.bind(parkingSpotCacheRetryProcQueue).to(parkingSpotCacheRetryExchange).with(MQConstant.PROC_ROUTING_KEY);
    }

    @Bean
    public Queue parkingSpotCacheRetryAlertQueue() {
        return new Queue(MQConstant.PARKING_SPOT_CACHE_RETRY_ALERT_QUEUE, true);
    }

    @Bean
    public Binding parkingSpotCacheRetryAlertBinding(Queue parkingSpotCacheRetryAlertQueue, DirectExchange parkingSpotCacheRetryExchange) {
        return BindingBuilder.bind(parkingSpotCacheRetryAlertQueue).to(parkingSpotCacheRetryExchange).with(MQConstant.ALERT_ROUTING_KEY);
    }

    /*预约*/
    @Bean
    public DirectExchange bookingExchange() {
        return new DirectExchange(MQConstant.BOOKING_EXCHANGE);
    }

    @Bean
    public Queue bookingNotifyQueue() {
        return new Queue(MQConstant.BOOKING_NOTIFY_QUEUE, true);
    }

    @Bean
    public Queue bookingDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", OrderConstant.RESERVED_ORDER_TTL_MIN * 60 * 1000);
        args.put("x-dead-letter-exchange", MQConstant.BOOKING_EXCHANGE);
        args.put("x-dead-letter-routing-key", MQConstant.BOOKING_EXPIRE_ROUTING_KEY);
        return new Queue(MQConstant.BOOKING_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue bookingExpireQueue() {
        return new Queue(MQConstant.BOOKING_EXPIRE_QUEUE, true);
    }

    @Bean
    public Binding bookingNotifyBinding(Queue bookingNotifyQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(bookingNotifyQueue).to(bookingExchange).with(MQConstant.BOOKING_NOTIFY_ROUTING_KEY);
    }

    @Bean
    public Binding bookingDelayBinding(Queue bookingDelayQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(bookingDelayQueue).to(bookingExchange).with(MQConstant.BOOKING_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding bookingExpireBinding(Queue bookingExpireQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(bookingExpireQueue).to(bookingExchange).with(MQConstant.BOOKING_EXPIRE_ROUTING_KEY);
    }
}
