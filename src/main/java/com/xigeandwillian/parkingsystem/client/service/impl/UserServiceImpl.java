package com.xigeandwillian.parkingsystem.client.service.impl;

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
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import com.xigeandwillian.parkingsystem.common.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Override
    public Result sendCode(String phone) {
        redis.opsForValue().set(RedisConstant.USER_PHONE_CODE_KEY+phone, phone);
        return Result.ok();
    }

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
        String key=RedisConstant.USER_PHONE_CODE_KEY+registerDTO.getPhone();
        String code = redis.opsForValue().get(key);
//        String code ="123456";
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
