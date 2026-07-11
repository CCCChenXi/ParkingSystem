package com.xigeandwillian.parkingsystem.common.interceptor;

import com.xigeandwillian.parkingsystem.common.constant.HttpConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.result.CacheResult;
import com.xigeandwillian.parkingsystem.common.service.RedisService;
import com.xigeandwillian.parkingsystem.common.utils.AdminHolder;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = request.getHeader(HttpConstant.AUTH_HEADER);

        if(token==null) {
            response.setStatus(ResultConstant.UNAUTHORIZED);
            return false;
        }
        if (token.startsWith(HttpConstant.TOKEN_PREFIX)) {
            token = token.substring(HttpConstant.TOKEN_PREFIX.length());
        }

        try {
            log.info("校验token:{}", token);
            Long adminId = Long.valueOf(jwtUtil.getSubject(token));

            CacheResult<Boolean> sessionResult = redisService.hasKey(
                    RedisConstant.Auth.ADMIN_SESSION_PREFIX + adminId);
            if (sessionResult.isError()) {
                log.error("验证管理员session失败: adminId={}", adminId);
                response.setStatus(ResultConstant.UNAUTHORIZED);
                return false;
            }
            if (sessionResult.isMiss() || Boolean.FALSE.equals(sessionResult.getData())) {
                log.warn("管理员session不存在，需重新登录: adminId={}", adminId);
                response.setStatus(ResultConstant.UNAUTHORIZED);
                return false;
            }

            AdminHolder.save(adminId);
            return true;
        } catch (Exception e) {
            response.setStatus(ResultConstant.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        AdminHolder.remove();
    }
}
