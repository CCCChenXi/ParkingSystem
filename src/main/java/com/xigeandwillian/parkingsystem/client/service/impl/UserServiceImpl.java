package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.userLoginDTO;
import com.xigeandwillian.parkingsystem.client.mapper.UserMapper;
import com.xigeandwillian.parkingsystem.client.service.Service.UserService;
import com.xigeandwillian.parkingsystem.client.vo.user.AuthorizeVO;
import com.xigeandwillian.parkingsystem.client.vo.user.UserVO;
import com.xigeandwillian.parkingsystem.common.constant.JwtClaimsConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.User;
import com.xigeandwillian.parkingsystem.common.exception.RegisterFailedException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import com.xigeandwillian.parkingsystem.common.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Override
    public Result sendCode(String phone) {
        String code = RandomUtil.randomNumbers(6);
        try {
            String key=RedisConstant.USER_PHONE_CODE+phone;
            redis.delete(key);
            redis.opsForValue().set(key,code,RedisConstant.USER_CODE_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw e;
        }
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result register(RegisterDTO registerDTO) {
        log.info("用户注册：{}",registerDTO);
        if(!RegexUtils.isValidPhone(registerDTO.getPhone())){
            log.warn("手机号格式错误");
            throw new RegisterFailedException(ResultConstant.BAD_REQUEST,"手机号格式错误");
        }

        String phone = registerDTO.getPhone();
        String key = RedisConstant.REGISTER_PHONE+phone;

        //查询是否已经被注册
        try {
            if(redis.hasKey(key)){
                //重置被记录手机号过期时间
                redis.expire(key,RedisConstant.REGISTER_PHONE_TTL, TimeUnit.MILLISECONDS);
                log.warn("手机号已经被注册了~");
                throw new RegisterFailedException(ResultConstant.BAD_REQUEST,"手机号已经被注册了~");
            }
        }
        catch (RegisterFailedException e){
            //业务异常，抛出
            throw e;
        }
        catch (RuntimeException e) {
            //服务器异常，放行
            log.error("redis服务器异常");
        }

        String code = null;
        try {
            //获取验证码
            code = redis.opsForValue().get(RedisConstant.USER_PHONE_CODE+registerDTO.getPhone());
        } catch (Exception e) {
            log.error("redis获取验证码失败!");
            throw new RuntimeException("安全认证服务暂时不可用，请稍后在试");
        }

        if(code==null){
            log.info("验证码过期");
            throw new RegisterFailedException(ResultConstant.BAD_REQUEST,"验证码过期，请重试");
        }
        if(!Objects.equals(code, registerDTO.getCode())){
            log.info("验证码错误");
            throw new RegisterFailedException(ResultConstant.BAD_REQUEST,"验证码错误");
        }

        User user = new User();
        //组装实体类
        BeanUtils.copyProperties(registerDTO,user);
        //对密码加密
        user.setPassword(DigestUtils.md5DigestAsHex(registerDTO.getPassword().getBytes(StandardCharsets.UTF_8)));

        //尝试插入一条数据，手机号不重复则不会出现异常，表示插入成功
        try {
            userMapper.insert(user);
        }
        catch (DuplicateKeyException e){
            //手机号重复
            throw new RegisterFailedException(ResultConstant.BAD_REQUEST,"手机号已经被注册了~");
        }
        catch (Exception e){
            //数据库|redis服务器异常
            throw new RuntimeException("服务器异常，请稍后重试");
        }
        try {
            //验证成功，删除验证码，防止被重复使用
            redis.delete(RedisConstant.USER_PHONE_CODE+registerDTO.getPhone());
            //标记手机号被注册
            redis.opsForValue().set(RedisConstant.REGISTER_PHONE+registerDTO.getPhone(),"1",RedisConstant.REGISTER_PHONE_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis服务器异常");
        }
        //生成token,封装返回数据
        String token= jwtUtil.createJWT(user.getId().toString());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        log.info("注册成功!");
        return Result.ok(new AuthorizeVO(token,userVO));
    }

    @Override
    public Result login(userLoginDTO userLoginDTO) {

        //前端传递的用户登录信息
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        //获取用户
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username)
        );
        if (user == null) {
            return Result.fail(ResultConstant.BAD_REQUEST, "用户不存在!");
        }

        //对前端传递的密码进行md5加密
        String secretPassword = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        if (!secretPassword.equals(user.getPassword())) {
            return Result.fail(ResultConstant.BAD_REQUEST, "密码错误!");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        Map<String, Object> claims = new HashMap<>();
        //1.插入数据
        claims.put(JwtClaimsConstant.USER_NAME, userVO.getUsername());
        claims.put(JwtClaimsConstant.USER_ID, userVO.getId());
        String token = jwtUtil.createJWT(userVO.getUsername(), claims);
        //封装VO
        return Result.ok(new AuthorizeVO(token, userVO));
    }

}
