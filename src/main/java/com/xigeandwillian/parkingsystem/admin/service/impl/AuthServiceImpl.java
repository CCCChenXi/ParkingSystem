package com.xigeandwillian.parkingsystem.admin.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.admin.dto.auth.LoginDTO;
import com.xigeandwillian.parkingsystem.admin.dto.auth.ProfileUpdateDTO;
import com.xigeandwillian.parkingsystem.admin.mapper.AuthMapper;
import com.xigeandwillian.parkingsystem.admin.service.AuthService;
import com.xigeandwillian.parkingsystem.admin.vo.auth.AdminVO;
import com.xigeandwillian.parkingsystem.common.mapper.AdminConverter;
import com.xigeandwillian.parkingsystem.admin.vo.auth.AuthorizeVO;
import com.xigeandwillian.parkingsystem.common.constant.JwtClaimsConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.Admin;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.CacheResult;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.service.RedisService;
import com.xigeandwillian.parkingsystem.common.utils.AdminHolder;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RedisService redisService;
    private final AuthMapper authMapper;
    private final JwtUtil jwtUtil;
    private final AdminConverter adminConverter;

    @Override
    public Result login(LoginDTO loginDTO) {
        String name = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        String key = RedisConstant.Auth.ADMIN_LOGIN_COUNT + name;
        String lockKey = RedisConstant.Auth.ADMIN_LOGIN_LOCK + name;

        CacheResult<Boolean> isLockResult = redisService.hasKey(lockKey);
        if (isLockResult.isError()) {
            log.error("检测登录锁定失败");
            throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");
        }
        if (isLockResult.isHit() && Boolean.TRUE.equals(isLockResult.getData())) {
            throw new BusinessException(ResultConstant.UNAUTHORIZED, "错误尝试次数过多，请稍后再试!");
        }

        Admin admin = authMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getUsername, name));
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        if (admin == null || !encryptedPassword.equals(admin.getPassword())) {
            log.warn("用户名或密码错误");
            CacheResult<Long> countResult = redisService.increment(key);
            if (countResult.isError()) {
                log.error("记录登录次数失败");
                throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");
            }
            long count = countResult.getData();

            if (count == 1) {
                if (redisService.expire(key, RedisConstant.Auth.ADMIN_LOGIN_ERROR_RESET_TTL_DAY, TimeUnit.DAYS).isError()) {
                    log.error("设置计数过期失败");
                    throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");
                }
            }

            if (count >= RedisConstant.Auth.ADMIN_LOGIN_ERROR_LIMIT) {
                redisService.delete(key);
                redisService.set(lockKey, "1", RedisConstant.Auth.ADMIN_LOGIN_ERROR_TTL_MIN, TimeUnit.MINUTES);
            }

            throw new BusinessException(ResultConstant.UNAUTHORIZED, "用户名或密码错误");
        }

        redisService.delete(key);

        String token = jwtUtil.createJWT(admin.getId().toString(), Map.of(JwtClaimsConstant.ROLE, JwtClaimsConstant.ROLE_ADMIN));
        AdminVO adminVO = adminConverter.toVO(admin);
        if (redisService.set(RedisConstant.Auth.ADMIN_SESSION_PREFIX + admin.getId(), JSONUtil.toJsonStr(adminVO), RedisConstant.Auth.ADMIN_SESSION_TTL_HOUR, TimeUnit.HOURS).isError()) {
            log.error("保存管理员session失败: adminId={}", admin.getId());
            throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");
        }

        return Result.ok(new AuthorizeVO(token, adminVO));
    }

    @Override
    public Result logout() {
        Long adminId = AdminHolder.get();
        if (adminId != null) {
            redisService.delete(RedisConstant.Auth.ADMIN_SESSION_PREFIX + adminId);
            log.info("管理员退出登录: adminId={}", adminId);
        }
        return Result.ok();
    }

    @Override
    public Result updateProfile(ProfileUpdateDTO dto) {
        Admin admin = authMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getUsername, dto.getUsername()));
        if (admin == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "管理员不存在");
        }

        String oldEncrypted = DigestUtils.md5DigestAsHex(
                dto.getOldPassword().getBytes(StandardCharsets.UTF_8));
        if (!oldEncrypted.equals(admin.getPassword())) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "原密码错误");
        }

        String newEncrypted = DigestUtils.md5DigestAsHex(
                dto.getNewPassword().getBytes(StandardCharsets.UTF_8));
        admin.setPassword(newEncrypted);
        authMapper.updateById(admin);

        log.info("修改密码成功: username={}", dto.getUsername());
        return Result.ok();
    }
}
