package com.xigeandwillian.parkingsystem.client.service.impl;

import com.fasterxml.jackson.databind.util.BeanUtil;
import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.mapper.UserMapper;
import com.xigeandwillian.parkingsystem.client.service.UserService;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.User;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.RegexUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;

    @Override
    public Result register(RegisterDTO registerDTO) {
        if(registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty()){
            return Result.fail(ResultConstant.BAD_REQUEST,"用户名不能为空");
        }
        if(registerDTO.getPassword() == null || registerDTO.getPassword().trim().isEmpty()){
            return Result.fail(ResultConstant.BAD_REQUEST,"密码不能为空");
        }
        if(!RegexUtils.isValidPhone(registerDTO.getPhone())){
            return Result.fail(ResultConstant.BAD_REQUEST,"手机号格式错误");
        }
        if(registerDTO.getCode()==null|| registerDTO.getCode().trim().isEmpty()){
            return Result.fail(ResultConstant.BAD_REQUEST,"验证码不能为空");
        }
//        String code = redis.opsForValue().get(RedisConstant.USER_PHONE_CODE_KEY+registerDTO.getPhone());
        String code ="123456";
        if(code==null){
            return Result.fail(ResultConstant.BAD_REQUEST,"验证码过期");
        }
        if(!Objects.equals(code, registerDTO.getCode())){
            return Result.fail(ResultConstant.BAD_REQUEST,"验证码错误");
        }
        User  user = new User();
//        BeanUtils.copyProperties(registerDTO,user);
//        return Result.ok(userMapper.insert(user));
        return Result.ok();
    }
}
