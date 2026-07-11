package com.xigeandwillian.parkingsystem.admin.controller;

import com.xigeandwillian.parkingsystem.admin.dto.auth.LoginDTO;
import com.xigeandwillian.parkingsystem.admin.dto.auth.ProfileUpdateDTO;
import com.xigeandwillian.parkingsystem.admin.service.AuthService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/admin")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result login(@Validated @RequestBody LoginDTO loginDTO) {
        log.info("管理员登录：{}", loginDTO.getUsername());
        return authService.login(loginDTO);
    }

    @PostMapping("/logout")
    public Result logout() {
        log.info("管理员退出登录");
        return authService.logout();
    }

    @PutMapping("/profile")
    public Result updateProfile(@Validated @RequestBody ProfileUpdateDTO dto) {
        log.info("修改密码: username={}", dto.getUsername());
        return authService.updateProfile(dto);
    }
}
