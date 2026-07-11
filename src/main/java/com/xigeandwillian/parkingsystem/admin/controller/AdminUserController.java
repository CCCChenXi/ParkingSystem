package com.xigeandwillian.parkingsystem.admin.controller;

import com.xigeandwillian.parkingsystem.admin.service.AdminUserService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping()
    public Result page(Integer page, Integer size, String keyword) {
        log.info("分页查询用户信息：page={}, size={}, keyword={}", page, size, keyword);
        return adminUserService.page(page, size, keyword);
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        log.info("查询用户详情: id={}", id);
        return adminUserService.detail(id);
    }
}
