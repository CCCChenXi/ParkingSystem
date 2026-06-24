package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.dto.userLoginDTO;
import com.xigeandwillian.parkingsystem.client.service.Service.LoginService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final LoginService loginService;

    @PostMapping("/send-code")
    public Result sendCode(@RequestBody String phone){
        return Result.ok();
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
