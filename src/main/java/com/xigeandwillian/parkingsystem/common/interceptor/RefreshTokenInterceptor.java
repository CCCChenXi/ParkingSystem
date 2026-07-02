package com.xigeandwillian.parkingsystem.common.interceptor;

import com.xigeandwillian.parkingsystem.common.constant.HttpConstant;
import com.xigeandwillian.parkingsystem.common.constant.JwtClaimsConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.service.service.RedisService;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            String token = request.getHeader(HttpConstant.AUTH_HEADER);
            if (token != null && token.startsWith(HttpConstant.TOKEN_PREFIX)) {
                token = token.substring(HttpConstant.TOKEN_PREFIX.length());
                try {
                    Claims claims = jwtUtil.parseJWT(token);
                    long id = Long.parseLong(claims.getSubject());
                    String role = claims.get(JwtClaimsConstant.ROLE, String.class);
                    if (JwtClaimsConstant.ROLE_ADMIN.equals(role)) {
                        redisService.expire(RedisConstant.Auth.ADMIN_SESSION_PREFIX + id,
                                RedisConstant.Auth.ADMIN_SESSION_TTL_HOUR, TimeUnit.HOURS);
                    } else if (JwtClaimsConstant.ROLE_USER.equals(role)) {
                        redisService.expire(RedisConstant.User.USER_SESSION_PREFIX + id,
                                RedisConstant.User.USER_SESSION_TTL_HOUR, TimeUnit.HOURS);
                    }
                } catch (Exception e) {
                    log.warn("Token刷新失败", e);
                }
            }
        }
        return true;
    }
}
