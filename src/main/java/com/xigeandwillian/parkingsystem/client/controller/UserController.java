package com.xigeandwillian.parkingsystem.client.controller;

import com.fasterxml.jackson.databind.util.BeanUtil;
import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.vo.user.ProfileVO;
import com.xigeandwillian.parkingsystem.client.vo.user.RegisterVO;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/user")
@RestController
public class UserController {

    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        ProfileVO user = new ProfileVO();
        BeanUtils.copyProperties(registerDTO, user);
        user.setId(1L);
        RegisterVO registerVO = new RegisterVO("token", user);
        return Result.ok(registerVO);
    }

    @PostMapping("/send-code")
    public Result sendCode(@RequestBody String phone){
        return Result.ok();
    }
}
