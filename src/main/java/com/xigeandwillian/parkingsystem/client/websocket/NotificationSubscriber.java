package com.xigeandwillian.parkingsystem.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xigeandwillian.parkingsystem.client.vo.message.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSubscriber implements MessageListener {

    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            Long userId = ((Number) data.get("userId")).longValue();
            String title = (String) data.get("title");
            String content = (String) data.get("content");
            Integer type = (Integer) data.get("type");

            MessageVO messageVO = new MessageVO();
            messageVO.setId(0L);
            messageVO.setTitle(title);
            messageVO.setContent(content);
            messageVO.setType(type);
            messageVO.setIsRead(0);
            messageVO.setCreateTime(LocalDateTime.now());

            webSocketHandler.pushToUser(userId, messageVO);
        } catch (Exception e) {
            log.error("处理Redis通知消息失败", e);
        }
    }
}
