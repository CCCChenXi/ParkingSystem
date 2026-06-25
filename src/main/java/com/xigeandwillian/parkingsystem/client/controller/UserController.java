package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.userLoginDTO;
import com.xigeandwillian.parkingsystem.client.service.Service.UserService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final UserService loginService;
    private final UserService userService;


    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        log.info("用户注册: {}", registerDTO);
        return userService.register(registerDTO);
    }

    @PostMapping("/send-code")
    public Result sendCode(@RequestBody String phone){

        return userService.sendCode(phone);
    }
    /**
     * 登录
     * @author willian
     * @param userLoginDtTO
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody userLoginDTO userLoginDtTO) {
        return loginService.login(userLoginDtTO);
    }
}
