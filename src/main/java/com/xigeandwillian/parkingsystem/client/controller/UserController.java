package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.dto.userLoginDTO;
import com.xigeandwillian.parkingsystem.client.service.Service.LoginService;
import com.xigeandwillian.parkingsystem.client.vo.LoginVO;
import com.xigeandwillian.parkingsystem.client.vo.UserVO;
import com.xigeandwillian.parkingsystem.common.constant.JwtClaimsConstant;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final LoginService loginService;

    /**
     * 登录
     * @author willian
     * @param userLoginDtTO
     * @return
     */
    @PostMapping("/user/login")
    public Result login(@RequestBody userLoginDTO userLoginDtTO) {
        return loginService.login(userLoginDtTO);
    }
}
