package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.client.dto.user.ProfileEditDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.LoginDTO;
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
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

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
        stringRedisTemplate.opsForValue().set(RedisConstant.USER_PHONE_CODE + phone, code);
        return Result.ok();
    }


    /**
     * 注册
     *
     * @param registerDTO
     * @return
     */
    @Override
    public Result register(RegisterDTO registerDTO) {
        if (registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty()) {
            return Result.fail(ResultConstant.BAD_REQUEST, "用户名不能为空");
        }
        if (registerDTO.getPassword() == null || registerDTO.getPassword().trim().isEmpty()) {
            return Result.fail(ResultConstant.BAD_REQUEST, "密码不能为空");
        }
        if (!RegexUtils.isValidPhone(registerDTO.getPhone())) {
            return Result.fail(ResultConstant.BAD_REQUEST, "手机号格式错误");
        }
        if (registerDTO.getCode() == null || registerDTO.getCode().trim().isEmpty()) {
            return Result.fail(ResultConstant.BAD_REQUEST, "验证码不能为空");
        }
        String key = RedisConstant.USER_PHONE_CODE + registerDTO.getPhone();
        String code = stringRedisTemplate.opsForValue().get(key);
//        String code ="123456";
        if (code == null) {
            return Result.fail(ResultConstant.BAD_REQUEST, "验证码过期");
        }
        if (!Objects.equals(code, registerDTO.getCode())) {
            return Result.fail(ResultConstant.BAD_REQUEST, "验证码错误");
        }
        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes(StandardCharsets.UTF_8)));
        return Result.ok(userMapper.insert(user));
        //return Result.ok();
    }

    /**
     * 登录
     *
     * @param LoginDTO
     * @return
     */
    @Override
    public Result login(LoginDTO LoginDTO) {

        //前端传递的用户登录信息
        String username = LoginDTO.getUsername();
        String password = LoginDTO.getPassword();

        //前端数据判空
        if (username == null || username.trim().isEmpty()) {
            return Result.fail(ResultConstant.BAD_REQUEST, "用户名不能为空!");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.fail(ResultConstant.BAD_REQUEST, "密码不能为空!");
        }


        //用户被禁止登录
        if (stringRedisTemplate.hasKey("login:lock:" + username)) {
            return Result.fail(ResultConstant.BAD_REQUEST, "账户或密码错误!");
        }

        //获取用户
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username)
        );
        //对前端传递的密码进行md5加密
        String secretPassword = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        //用户登录次数key
        String key=RedisConstant.USER_LOGIN_COUNT+username;

        if (user == null || !secretPassword.equals(user.getPassword())) {
            //获取尝试次数
            Long count=stringRedisTemplate.opsForValue().increment(key);
            //对于第一次尝试,设置过期时间,使得一段时间后,计数器清零
            if(count==1){
                stringRedisTemplate.expire(key,864000000,TimeUnit.MILLISECONDS);
            }
            if(count>=5){
                //禁止登录五分钟
                stringRedisTemplate.opsForValue().set("login:lock:" + username,"1",300000,TimeUnit.MILLISECONDS);
                //删除计数器
                stringRedisTemplate.delete(key);
            }
            return Result.fail(ResultConstant.BAD_REQUEST, "账户或密码错误!");
        }


        //生成token
        Long userId = user.getId();
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, userId);
        String token = jwtUtil.createJWT(user.getUsername(), claims);
        //清空登录次数缓存
        stringRedisTemplate.delete(key);
        //封装VO
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        //缓存VO方便取用减少多次缓存数据库
        stringRedisTemplate
                .opsForValue()
                .set(RedisConstant.USER_VO + userId, JSONUtil.toJsonStr(userVO), RedisConstant.USER_VO_TTL, TimeUnit.MINUTES);
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
        String s = stringRedisTemplate.opsForValue().get(RedisConstant.USER_VO + userId);
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
        //获取原用户信息
        long userId = UserHolder.get();
        //对修改信息判断
        //1.手机号判空
        if (profileEditDTO.getPhone() == null || profileEditDTO.getPhone().trim().isEmpty()) {
            return Result.fail(ResultConstant.BAD_REQUEST, "手机号不能为空!");
        }
        //2.手机号格式判断
        if (!RegexUtils.isValidPhone(profileEditDTO.getPhone())) {
            return Result.fail(ResultConstant.BAD_REQUEST, "请填写正确的手机号格式!");
        }


        //月修改次数判断
        try {
            String str = stringRedisTemplate.opsForValue().get(RedisConstant.USER_EDIT_TIMES + userId);
            long times = str == null ? 0 : Long.parseLong(str);

            if (times >= 2) {
                return Result.fail(ResultConstant.BAD_REQUEST, "本月已修改个人信息两次!");
            }
        } catch (Exception e) {
            log.error("redis服务器异常~");
        }

        //修改数据库数据及覆盖缓存
        //1.更新数据库信息
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getPhone, profileEditDTO.getPhone())
                .set(User::getAvatar, profileEditDTO.getAvatar()));
        //2更新缓存信息
        UserVO newUserVO = new UserVO();
        User newUser = userMapper.selectById(userId);
        BeanUtils.copyProperties(newUser, newUserVO);
        try {
            stringRedisTemplate.opsForValue().set(RedisConstant.USER_VO + userId, JSONUtil.toJsonStr(newUserVO), RedisConstant.USER_VO_TTL, TimeUnit.MINUTES);
            Long increment = stringRedisTemplate.opsForValue().increment(RedisConstant.USER_EDIT_TIMES + userId);
            if (increment == 1) {
                stringRedisTemplate.expire(RedisConstant.USER_EDIT_TIMES + userId, 30, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("redis服务器异常~");
        }
        return Result.ok(newUserVO);
    }

    /**
     * 车辆信息
     *
     * @return
     */
    @Override
    public Result vehiclesInfo() {
        long userId = UserHolder.get();
        return Result.ok();
    }

}
