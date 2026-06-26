package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.dto.user.CodeDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.ProfileEditDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.LoginDTO;
import com.xigeandwillian.parkingsystem.client.service.Service.UserService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 注册
     * @author xige
     * @param registerDTO
     * @return
     */
    @PostMapping("/register")
    public Result register(@Validated @RequestBody RegisterDTO registerDTO) {
        log.info("用户注册: {}", registerDTO);
        return userService.register(registerDTO);
    }

    /**
     * 发送验证码
     * @author xige
     * @param code
     * @return
     */
    @PostMapping("/send-code")
    public Result sendCode(@RequestBody CodeDTO code){
        return userService.sendCode(code.getPhone());
    }
    /**
     * 登录
     * @author willian
     * @param userLoginDtTO
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO userLoginDtTO) {
        return userService.login(userLoginDtTO);
    }

    /**
     * 获取用户信息
     * @author willian
     * @return
     */
    @GetMapping("/profile")
    public Result profile(){
        return userService.userProfile();
    }
    /**
     * 修改用户信息
     * @author willian
     * @return
     */
    @PutMapping("/profile")
    public Result editProfile(@RequestBody ProfileEditDTO profileEditDTO){
        return userService.editProfile(profileEditDTO);
    }

    @GetMapping("/vehicles")
    public Result vehiclesInfo(){
        return userService.vehiclesInfo();

    }

}
