package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.dto.user.CodeDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.ProfileEditDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.RegisterDTO;
import com.xigeandwillian.parkingsystem.client.dto.user.LoginDTO;
import com.xigeandwillian.parkingsystem.client.service.UserService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 注册
     *
     * @param registerDTO
     * @return
     * @author xige
     */
    @PostMapping("/register")
    public Result register(@Validated @RequestBody RegisterDTO registerDTO) {
        log.info("用户注册: username={}, phone={}", registerDTO.getUsername(), registerDTO.getPhone());
        return userService.register(registerDTO);
    }

    /**
     * 发送验证码
     *
     * @param code
     * @return
     * @author xige
     */
    @PostMapping("/send-code")
    public Result sendCode(@Validated @RequestBody CodeDTO code) {
        log.info("发送验证码:{}", code.getPhone());
        return userService.sendCode(code.getPhone());
    }

    /**
     * 登录
     *
     * @param userLoginDtTO
     * @return
     * @author willian
     */
    @PostMapping("/login")
    public Result login(@Validated @RequestBody LoginDTO userLoginDtTO) {
        log.info("用户登录: {}", userLoginDtTO.getUsername());
        return userService.login(userLoginDtTO);
    }

    /**
     * 获取用户信息
     *
     * @return
     * @author willian
     */
    @GetMapping("/profile")
    public Result profile() {
        log.info("获取用户信息");
        return userService.userProfile();
    }

    /**
     * 修改用户信息
     *
     * @return
     * @author willian
     */
    @PutMapping("/profile")
    public Result editProfile(@Validated @RequestBody ProfileEditDTO profileEditDTO) {
        log.info("修改用户信息: {}", profileEditDTO);
        return userService.editProfile(profileEditDTO);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result logout() {
        log.info("用户退出登录");
        return userService.logout();
    }

}
