package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.admin.dto.admin.LoginDTO;
import com.xigeandwillian.parkingsystem.admin.mapper.AdminMapper;
import com.xigeandwillian.parkingsystem.admin.service.Service.AdminService;
import com.xigeandwillian.parkingsystem.admin.vo.admin.AdminVO;
import com.xigeandwillian.parkingsystem.admin.vo.admin.AuthorizeVO;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.Admin;
import com.xigeandwillian.parkingsystem.common.exception.LoginFailedException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final StringRedisTemplate redis;
    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;

    @Override
    public Result login(LoginDTO loginDTO) {
        String name = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        //用户登录次数key
        String key = RedisConstant.ADMIN_LOGIN_COUNT + name;

        //错误次数及格被禁止登录的用户key
        String errorKey = RedisConstant.ADMIN_ERROR_LOCK + name;

        //如果redis宕机，降级处理，保证服务正常运行
        try {
            //判断是否被禁止登录
            if (redis.hasKey(errorKey)) {
                throw new LoginFailedException(ResultConstant.UNAUTHORIZED, "错误尝试次数过多，请稍后再试!");
            }
        } catch (LoginFailedException e) {
            //禁止登录，不放行
            throw e;
        } catch (Exception e) {
            //服务器异常，降级访问数据库
            log.error("redis服务器异常");
        }


        //查询用户
        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getUsername, name));

        //加密后的密码
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        //用户不存在 或者 密码错误
        if (admin == null || !encryptedPassword.equals(admin.getPassword())) {

            log.warn("用户名或密码错误");
            //如果redis异常，则跳过
            try {
                //获取登录错误次数+1
                Long count = redis.opsForValue().increment(key);

                //count==null,redis异常，跳过判断，降级查数据库
                if (count == 1) {
                    //从第一次登录开始，记录登录错误错误次数，并在第二天重置错误次数
                    redis.expire(key, RedisConstant.LOGIN_ERROR_RESET_TTL, TimeUnit.MILLISECONDS);
                }

                //如果count==null,redis异常，跳过判断
                if (count >= RedisConstant.LOGIN_ERROR_LIMIT) {
                    redis.delete(key);
                    redis.opsForValue().set(errorKey, "1", RedisConstant.LOGIN_ERROR_TTL, TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                log.error("redis服务器异常，计数失败");
            }

            throw new LoginFailedException(ResultConstant.UNAUTHORIZED,"用户名或密码错误");
        }
        //登录成功，删除计数器
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.error("redis服务器异常");
        }

        //密码正确，封装需要返回数据
        //获取token
        String token = jwtUtil.createJWT(admin.getId().toString());
        AdminVO adminVO = new AdminVO();

        BeanUtils.copyProperties(admin, adminVO);

        return Result.ok(new AuthorizeVO(token, adminVO));
    }

}
