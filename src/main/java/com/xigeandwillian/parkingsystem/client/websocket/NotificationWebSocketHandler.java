package com.xigeandwillian.parkingsystem.client.websocket;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xigeandwillian.parkingsystem.client.vo.message.MessageVO;
import com.xigeandwillian.parkingsystem.client.vo.message.PushMessageVO;
import com.xigeandwillian.parkingsystem.client.vo.message.SyncMessageVO;
import com.xigeandwillian.parkingsystem.common.entity.Message;
import com.xigeandwillian.parkingsystem.common.mapper.MessageConverter;
import com.xigeandwillian.parkingsystem.common.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, CopyOnWriteArraySet<WebSocketSession>> userSessionMap = new HashMap<>();
    private static final int MAX_SYNC_COUNT = 100;

    private final MessageMapper messageMapper;
    private final MessageConverter messageConverter;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        userSessionMap.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);

        Map<String, String> params = parseQueryParams(session.getUri().getQuery());
        long lastTimestamp = Long.parseLong(params.getOrDefault("lastTimestamp", "0"));
        long lastId = Long.parseLong(params.getOrDefault("lastId", "0"));

        log.info("WebSocket:车主{}连接成功，当前连接数{}，断线重连参数{},{}",
                userId, userSessionMap.get(userId).size(), lastTimestamp, lastId);

        syncMessages(session, userId, lastTimestamp, lastId);
    }

    private void syncMessages(WebSocketSession session, Long userId, long lastTimestamp, long lastId) {
        LocalDateTime lct = lastTimestamp > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTimestamp), ZoneId.systemDefault())
                : LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

        List<Message> messages = messageMapper.selectList(
                Wrappers.<Message>lambdaQuery()
                        .eq(Message::getUserId, userId)
                        .and(w -> w.gt(Message::getCreateTime, lct)
                                .or(w2 -> w2.eq(Message::getCreateTime, lct)
                                        .gt(Message::getId, lastId)))
                        .orderByDesc(Message::getCreateTime)
                        .orderByDesc(Message::getId)
                        .last("LIMIT " + MAX_SYNC_COUNT));

        if (messages.isEmpty()) {
            SyncMessageVO resp = new SyncMessageVO();
            resp.setMessages(Collections.emptyList());
            resp.setLastTimestamp(0L);
            resp.setLastId(0L);
            sendJson(session, resp);
            return ;
        }

        List<MessageVO> voList = messages.stream().map(this::toVO).toList();
        MessageVO last = voList.get(0);

        SyncMessageVO resp = new SyncMessageVO();
        resp.setMessages(voList);
        resp.setLastTimestamp(last.getCreateTime()
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        resp.setLastId(last.getId());

        sendJson(session, resp);
    }

    public void pushToUser(Long userId, MessageVO message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessionMap.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        PushMessageVO resp = new PushMessageVO();
        resp.setMessage(message);
        resp.setTimestamp(message.getCreateTime()
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        resp.setId(message.getId());

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) sendJson(session, resp);
        }
    }

    private void sendJson(WebSocketSession session, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("WebSocket发送消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = userSessionMap.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessionMap.remove(userId);
                }
            }
        }
    }

    private MessageVO toVO(Message msg) {
        return messageConverter.toVO(msg);
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }
}
