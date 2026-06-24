package com.xigeandwillian.parkingsystem.client.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.client.dto.userLoginDTO;
import com.xigeandwillian.parkingsystem.client.mapper.userMapper;
import com.xigeandwillian.parkingsystem.client.service.Service.LoginService;
import com.xigeandwillian.parkingsystem.client.vo.user.AuthorizeVO;
import com.xigeandwillian.parkingsystem.client.vo.user.UserVO;
import com.xigeandwillian.parkingsystem.common.constant.JwtClaimsConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.User;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private userMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 登录
     *
     * @param userLoginDTO 用户登录信息
     * @return
     */
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
        BeanUtil.copyProperties(user, userVO);
        Map<String, Object> claims = new HashMap<>();
        //1.插入数据
        claims.put(JwtClaimsConstant.USER_NAME, userVO.getUsername());
        claims.put(JwtClaimsConstant.USER_ID, userVO.getId());
        String token = jwtUtil.createJWT(userVO.getUsername(), claims);
        //封装VO
        return Result.ok(new AuthorizeVO(token, userVO));
    }
}
