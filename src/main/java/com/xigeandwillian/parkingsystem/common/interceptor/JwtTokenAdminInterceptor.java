package com.xigeandwillian.parkingsystem.common.interceptor;

import com.xigeandwillian.parkingsystem.common.constant.HttpConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
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
            AdminHolder.save(Long.valueOf(jwtUtil.getSubject(token)));
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
