package com.xigeandwillian.parkingsystem.common.config;

import com.xigeandwillian.parkingsystem.common.interceptor.JwtTokenAdminInterceptor;
import com.xigeandwillian.parkingsystem.common.interceptor.JwtTokenUserInterceptor;
import com.xigeandwillian.parkingsystem.common.interceptor.RefreshTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final RefreshTokenInterceptor refreshTokenInterceptor;
    private final JwtTokenUserInterceptor jwtTokenUserInterceptor;
    private final JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Token刷新拦截器(所有请求，刷新会话过期时间)
        registry.addInterceptor(refreshTokenInterceptor)
                .addPathPatterns("/**");

        // 用户拦截器(拦截公共接口外所有)
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/vehicles/**")
                .addPathPatterns("/user/**")
                .addPathPatterns("/coupons/**")
                .addPathPatterns("/wallet/**")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/user/register")
                .excludePathPatterns("/user/send-code");


        // 管理员拦截器(除了登录其余全拦截)
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
    }

}
