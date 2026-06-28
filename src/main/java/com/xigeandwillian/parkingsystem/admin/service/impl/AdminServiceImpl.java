package com.xigeandwillian.parkingsystem.admin.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.admin.dto.admin.LoginDTO;
import com.xigeandwillian.parkingsystem.admin.mapper.AdminMapper;
import com.xigeandwillian.parkingsystem.admin.service.Service.AdminService;
import com.xigeandwillian.parkingsystem.admin.vo.admin.AdminVO;
import com.xigeandwillian.parkingsystem.admin.vo.admin.AuthorizeVO;
import com.xigeandwillian.parkingsystem.common.constant.JwtClaimsConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.Admin;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.service.service.RedisService;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RedisService redisService;
    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;

    @Override
    public Result login(LoginDTO loginDTO) {
        String name = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        String key = RedisConstant.Admin.ADMIN_LOGIN_COUNT + name;
        String lockKey = RedisConstant.Admin.ADMIN_LOGIN_LOCK + name;

        Boolean isLock = redisService.hasKey(lockKey);
        if (isLock == null) {
            log.error("检测登录锁定失败");
//            throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");
        }
        if (isLock) {
            throw new BusinessException(ResultConstant.UNAUTHORIZED, "错误尝试次数过多，请稍后再试!");
        }

        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getUsername, name));
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        if (admin == null || !encryptedPassword.equals(admin.getPassword())) {
            log.warn("用户名或密码错误");
            Long count = redisService.increment(key);
            if (count == null) {
                log.error("记录登录次数失败");
                throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");
            }

            if (count == 1) {
                if (redisService.expire(key, RedisConstant.Admin.ADMIN_LOGIN_ERROR_RESET_TTL_DAY, TimeUnit.DAYS) == null) {
                    log.error("设置计数过期失败");
                    throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");
                }
            }

            if (count >= RedisConstant.Admin.ADMIN_LOGIN_ERROR_LIMIT) {
                redisService.delete(key);
                redisService.set(lockKey, "1", RedisConstant.Admin.ADMIN_LOGIN_ERROR_TTL_MIN, TimeUnit.MINUTES);
            }

            throw new BusinessException(ResultConstant.UNAUTHORIZED, "用户名或密码错误");
        }

        redisService.delete(key);

        String token = jwtUtil.createJWT(admin.getId().toString(), Map.of(JwtClaimsConstant.ROLE, JwtClaimsConstant.ROLE_ADMIN));
        AdminVO adminVO = new AdminVO();
        BeanUtils.copyProperties(admin, adminVO);
        redisService.set(RedisConstant.Admin.ADMIN_SESSION_PREFIX + admin.getId(), JSONUtil.toJsonStr(adminVO), RedisConstant.Admin.ADMIN_SESSION_TTL_HOUR, TimeUnit.HOURS);

        return Result.ok(new AuthorizeVO(token, adminVO));
    }

}
