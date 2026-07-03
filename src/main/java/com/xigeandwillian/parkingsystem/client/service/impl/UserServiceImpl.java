package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.client.dto.user.LoginDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.ProfileEditDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.mapper.UserMapper;
import com.xigeandwillian.parkingsystem.client.service.service.UserService;
import com.xigeandwillian.parkingsystem.client.vo.user.AuthorizeVO;
import com.xigeandwillian.parkingsystem.client.vo.user.UserVO;
import com.xigeandwillian.parkingsystem.common.constant.JwtClaimsConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.User;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.service.service.RedisService;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    /**
     * 发送验证码
     *
     * @param phone
     * @return
     */
    @Override
    public Result sendCode(String phone) {
        String code = RandomUtil.randomNumbers(6);
        log.info("发送验证码: {}", code);
        String key = RedisConstant.User.USER_PHONE_CODE + phone;
        try {
            stringRedisTemplate.opsForValue().set(key, code, RedisConstant.User.USER_CODE_TTL_MIN, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis服务器异常，保存验证码失败");
        }

        return Result.ok();
    }


    /**
     * 注册
     *
     * @param registerDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result register(RegisterDTO registerDTO) {

        String phone = registerDTO.getPhone();
        String key = RedisConstant.User.USER_REGISTER_PHONE + phone;

        Boolean registered = redisService.hasKey(key);
        if (registered == null) {
            log.error("redis服务器异常，查询注册状态失败");
        } else if (registered) {
            redisService.expire(key, RedisConstant.User.USER_REGISTER_PHONE_TTL_DAY, TimeUnit.DAYS);
            log.warn("手机号已经被注册了~");
            throw new BusinessException(ResultConstant.BAD_REQUEST, "手机号已经被注册了~");
        }

        String code = null;
        try {
            code = stringRedisTemplate.opsForValue().get(RedisConstant.User.USER_PHONE_CODE + phone);
        } catch (Exception e) {
            log.error("redis服务器异常，获取验证码失败");
            throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "安全认证服务暂时不可用，请稍后在试");
        }

        if (code == null) {
            log.warn("验证码过期");
            throw new BusinessException(ResultConstant.BAD_REQUEST, "验证码过期，请重试");
        }
        if (!Objects.equals(code, registerDTO.getCode())) {
            log.warn("验证码错误");
            throw new BusinessException(ResultConstant.BAD_REQUEST, "验证码错误");
        }

        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(DigestUtils.md5DigestAsHex(registerDTO.getPassword().getBytes(StandardCharsets.UTF_8)));

        // 利用手机号唯一约束，插入成功即注册、失败说明已注册
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "手机号已经被注册了~");
        } catch (Exception e) {
            throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "服务器异常，请稍后重试");
        }
        //验证成功，删除验证码，防止被重复使用
        redisService.delete(RedisConstant.User.USER_PHONE_CODE + phone);
        //标记手机号被注册
        redisService.set(key, "1", RedisConstant.User.USER_REGISTER_PHONE_TTL_DAY, TimeUnit.DAYS);
        String token = jwtUtil.createJWT(user.getId().toString(), Map.of(JwtClaimsConstant.ROLE, JwtClaimsConstant.ROLE_USER));
        UserVO userVO = new UserVO();

        BeanUtils.copyProperties(user, userVO);
        redisService.set(RedisConstant.User.USER_SESSION_PREFIX + user.getId(), JSONUtil.toJsonStr(userVO), RedisConstant.User.USER_SESSION_TTL_HOUR, TimeUnit.HOURS);

        log.info("注册成功!");
        return Result.ok(new AuthorizeVO(token, userVO));
    }

    /**
     * 登录
     *
     * @param loginDTO
     * @return
     */
    @Override
    public Result login(LoginDTO loginDTO) {

        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        String lockKey = RedisConstant.User.USER_LOGIN_LOCK + username;
        String key = RedisConstant.User.USER_LOGIN_COUNT + username;

        Boolean isLock = redisService.hasKey(lockKey);
        if (isLock == null) {
            log.error("检测登录锁定失败");
        }
        if (isLock != null && isLock) {
            throw new BusinessException(ResultConstant.UNAUTHORIZED, "错误尝试过多，请5分钟后再试!");
        }

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        String secretPassword = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        if (user == null || !secretPassword.equals(user.getPassword())) {
            Long count = redisService.increment(key);
            if (count == null) {
                log.error("记录登录次数失败");
                throw new BusinessException(ResultConstant.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");
            }

            if (count == 1) {
                if (redisService.expire(key, RedisConstant.User.USER_LOGIN_COUNT_TTL_DAY, TimeUnit.DAYS) == null) {
                    log.error("设置计数过期失败");
                }
            }

            if (count >= RedisConstant.User.USER_LOGIN_ERROR_LIMIT) {
                redisService.set(lockKey, "1", RedisConstant.User.USER_LOGIN_LOCK_TTL_MIN, TimeUnit.MINUTES);
                redisService.delete(key);
            }

            throw new BusinessException(ResultConstant.UNAUTHORIZED, "账户或密码错误!");
        }

        Long userId = user.getId();
        String token = jwtUtil.createJWT(user.getId().toString(), Map.of(JwtClaimsConstant.ROLE, JwtClaimsConstant.ROLE_USER));

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        redisService.delete(key);
        redisService.set(RedisConstant.User.USER_SESSION_PREFIX + userId, JSONUtil.toJsonStr(userVO), RedisConstant.User.USER_SESSION_TTL_HOUR, TimeUnit.HOURS);

        return Result.ok(new AuthorizeVO(token, userVO));
    }

    /**
     * 用户信息
     *
     * @return
     */
    @Override
    public Result userProfile() {
        long userId = UserHolder.get();
        String s = redisService.get(RedisConstant.User.USER_SESSION_PREFIX + userId);
        if (s == null) {
            throw new BusinessException(ResultConstant.UNAUTHORIZED, "用户信息不存在");
        }
        UserVO userVO = JSONUtil.toBean(s, UserVO.class);
        return Result.ok(userVO);
    }

    /**
     * 修改用户信息
     *
     * @param profileEditDTO
     * @return
     */
    @Override
    @Transactional
    public Result editProfile(ProfileEditDTO profileEditDTO) {
        long userId = UserHolder.get();
        String str = redisService.get(RedisConstant.User.USER_EDIT_TIMES + userId);
        long times = str == null ? 0 : Long.parseLong(str);

        if (times >= RedisConstant.User.USER_EDIT_LIMIT) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "本月已修改个人信息两次!");
        }

        // DB 更新 + 缓存覆盖（用户名不可修改）
        userMapper.update(null, Wrappers.<User>lambdaUpdate().eq(User::getId, userId).set(User::getPhone, profileEditDTO.getPhone()).set(User::getAvatar, profileEditDTO.getAvatar()));
        UserVO newUserVO = new UserVO();
        User newUser = userMapper.selectById(userId);
        BeanUtils.copyProperties(newUser, newUserVO);
        redisService.set(RedisConstant.User.USER_SESSION_PREFIX + userId, JSONUtil.toJsonStr(newUserVO), RedisConstant.User.USER_SESSION_TTL_HOUR, TimeUnit.HOURS);
        Long increment = redisService.increment(RedisConstant.User.USER_EDIT_TIMES + userId);
        if (increment != null && increment == 1) {
            redisService.expire(RedisConstant.User.USER_EDIT_TIMES + userId, RedisConstant.User.USER_EDIT_TIMES_TTL_DAY, TimeUnit.DAYS);
        }
        return Result.ok(newUserVO);
    }


}
