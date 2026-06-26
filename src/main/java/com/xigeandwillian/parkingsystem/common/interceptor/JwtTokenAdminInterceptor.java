package com.xigeandwillian.parkingsystem.common.interceptor;

import com.xigeandwillian.parkingsystem.client.vo.user.UserVO;
import com.xigeandwillian.parkingsystem.common.constant.JwtClaimsConstant;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import io.jsonwebtoken.Claims;
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

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //判断是否Controller方法资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //获取token
        String token = request.getHeader("Authorization");

        //去除Bearer
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }


        try {
            log.info("校验token:{}", token);
            //解析token
            Claims claims = jwtUtil.parseJWT(token);
            //保存用户
            UserHolder.save(claims.get(JwtClaimsConstant.USER_ID, Long.class));
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除信息
        UserHolder.remove();
    }
}
