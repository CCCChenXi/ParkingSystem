package com.xigeandwillian.parkingsystem.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long userId, String title, String content, Integer type) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("userId", userId);
            msg.put("title", title);
            msg.put("content", content);
            msg.put("type", type);
            msg.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(msg);
            stringRedisTemplate.convertAndSend(RedisConstant.Parking.NOTIFICATION_USER_CHANNEL, json);
            log.info("Redis通知已发送: userId={}, type={}", userId, type);
        } catch (Exception e) {
            log.warn("发送Redis通知失败: userId={}, type={}", userId, type, e);
        }
    }
}
