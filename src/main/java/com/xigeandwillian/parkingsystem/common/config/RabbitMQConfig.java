package com.xigeandwillian.parkingsystem.common.config;

import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String BOOKING_NOTIFY_QUEUE = "booking.notify.queue";
    public static final String BOOKING_NOTIFY_ROUTING_KEY = "booking.notify";
    public static final String BOOKING_DELAY_QUEUE = "booking.delay.queue";
    public static final String BOOKING_DELAY_ROUTING_KEY = "booking.delay";
    public static final String BOOKING_EXPIRE_QUEUE = "booking.expire.queue";
    public static final String BOOKING_EXPIRE_ROUTING_KEY = "booking.expire";

    public static final String CACHE_INVALIDATE_EXCHANGE = "cache.invalidate.exchange";

    public static final String SECKILL_QUEUE = "seckill.order.queue";
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

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

    /*预约*/

    //预约交换机
    @Bean
    public DirectExchange bookingExchange() {
        return new DirectExchange(BOOKING_EXCHANGE);
    }
    //预约消息队列
    @Bean
    public Queue bookingNotifyQueue() {
        return new Queue(BOOKING_NOTIFY_QUEUE, true);
    }

    //预约延迟队列
    @Bean
    public Queue bookingDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", OrderConstant.RESERVED_ORDER_TTL);
        args.put("x-dead-letter-exchange", BOOKING_EXCHANGE);
        args.put("x-dead-letter-routing-key", BOOKING_EXPIRE_ROUTING_KEY);
        return new Queue(BOOKING_DELAY_QUEUE, true, false, false, args);
    }

    //预约过期队列
    @Bean
    public Queue bookingExpireQueue() {
        return new Queue(BOOKING_EXPIRE_QUEUE, true);
    }

    //预约消息绑定
    @Bean
    public Binding bookingNotifyBinding(Queue bookingNotifyQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(bookingNotifyQueue).to(bookingExchange).with(BOOKING_NOTIFY_ROUTING_KEY);
    }


    //预约延迟消息绑定
    @Bean
    public Binding bookingDelayBinding(Queue bookingDelayQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(bookingDelayQueue).to(bookingExchange).with(BOOKING_DELAY_ROUTING_KEY);
    }

    //预约过期消息绑定
    @Bean
    public Binding bookingExpireBinding(Queue bookingExpireQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(bookingExpireQueue).to(bookingExchange).with(BOOKING_EXPIRE_ROUTING_KEY);
    }
}
