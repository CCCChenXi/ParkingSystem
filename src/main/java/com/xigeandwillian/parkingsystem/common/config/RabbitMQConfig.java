package com.xigeandwillian.parkingsystem.common.config;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_QUEUE = "seckill.order.queue";
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

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
        return new Queue(SECKILL_QUEUE, true);
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE);
    }

    @Bean
    public Binding seckillBinding(Queue seckillQueue, DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue).to(seckillExchange).with(SECKILL_ROUTING_KEY);
    }

    public static final String CACHE_INVALIDATE_EXCHANGE = "cache.invalidate.exchange";

    @Bean
    public FanoutExchange cacheInvalidateExchange() {
        return new FanoutExchange(CACHE_INVALIDATE_EXCHANGE);
    }

    @Bean
    public AnonymousQueue cacheInvalidateQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding cacheInvalidateBinding(AnonymousQueue cacheInvalidateQueue, FanoutExchange cacheInvalidateExchange) {
        return BindingBuilder.bind(cacheInvalidateQueue).to(cacheInvalidateExchange);
    }

    // ──── 车位缓存重试（保证最终成功） ────
    // 链路: afterCommit → Source(TTL=30s, DLX→RetryExchange) → Proc(消费者) → 成功广播 / 失败重发到Source / 用尽→Alert
    public static final String PARKING_SPOT_CACHE_RETRY_EXCHANGE = "parking.spot.cache.retry.exchange";
    public static final String PARKING_SPOT_CACHE_UPDATE_SOURCE_QUEUE = "parking.spot.cache.update.source.queue";
    public static final String PARKING_SPOT_CACHE_CREATE_SOURCE_QUEUE = "parking.spot.cache.create.source.queue";
    public static final String PARKING_SPOT_CACHE_RETRY_PROC_QUEUE = "parking.spot.cache.retry.proc.queue";
    public static final String PARKING_SPOT_CACHE_RETRY_ALERT_QUEUE = "parking.spot.cache.retry.alert.queue";

    @Bean
    public DirectExchange parkingSpotCacheRetryExchange() {
        return new DirectExchange(PARKING_SPOT_CACHE_RETRY_EXCHANGE);
    }

    @Bean
    public Queue parkingSpotCacheUpdateSourceQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", PARKING_SPOT_CACHE_RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", "proc");
        args.put("x-message-ttl", 30000);
        return new Queue(PARKING_SPOT_CACHE_UPDATE_SOURCE_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue parkingSpotCacheCreateSourceQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", PARKING_SPOT_CACHE_RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", "proc");
        args.put("x-message-ttl", 10000);
        return new Queue(PARKING_SPOT_CACHE_CREATE_SOURCE_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue parkingSpotCacheRetryProcQueue() {
        return new Queue(PARKING_SPOT_CACHE_RETRY_PROC_QUEUE, true);
    }

    @Bean
    public Binding parkingSpotCacheRetryProcBinding(Queue parkingSpotCacheRetryProcQueue, DirectExchange parkingSpotCacheRetryExchange) {
        return BindingBuilder.bind(parkingSpotCacheRetryProcQueue).to(parkingSpotCacheRetryExchange).with("proc");
    }

    @Bean
    public Queue parkingSpotCacheRetryAlertQueue() {
        return new Queue(PARKING_SPOT_CACHE_RETRY_ALERT_QUEUE, true);
    }

    @Bean
    public Binding parkingSpotCacheRetryAlertBinding(Queue parkingSpotCacheRetryAlertQueue, DirectExchange parkingSpotCacheRetryExchange) {
        return BindingBuilder.bind(parkingSpotCacheRetryAlertQueue).to(parkingSpotCacheRetryExchange).with("alert");
    }
}
