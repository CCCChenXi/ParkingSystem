package com.xigeandwillian.parkingsystem.common.interceptor;

import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
public class NotificationHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String query = request.getURI().getQuery();
        String token = extractTokenFromQuery(query);

        if (token == null) {
            log.warn("WebSocket握手失败: token为空, uri={}", request.getURI());
            response.setStatusCode(HttpStatusCode.valueOf(ResultConstant.UNAUTHORIZED));
            return false;
        }
        try {
            String subject = jwtUtil.getSubject(token);
            Long userId = Long.parseLong(subject);
            log.info("WebSocket握手: userId={}", userId);
            if (stringRedisTemplate.hasKey(RedisConstant.User.USER_SESSION_PREFIX + userId)) {
                attributes.put("userId", userId);
                return true;
            }
            log.warn("WebSocket握手失败: session不存在, userId={}", userId);
            response.setStatusCode(HttpStatusCode.valueOf(ResultConstant.UNAUTHORIZED));
            return false;
        } catch (Exception e) {
            log.error("WebSocket握手异常: token={}", token, e);
            response.setStatusCode(HttpStatusCode.valueOf(ResultConstant.UNAUTHORIZED));
            return false;
        }
    }

    private String extractTokenFromQuery(String query) {
        if (query == null || query.isBlank()) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0]) && !kv[1].isBlank()) {
                return kv[1];
            }
        }
        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
