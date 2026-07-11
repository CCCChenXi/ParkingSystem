package com.xigeandwillian.parkingsystem.admin.controller;

import com.xigeandwillian.parkingsystem.admin.dto.auth.AdminCreateDTO;
import com.xigeandwillian.parkingsystem.admin.service.AdminManageService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/admins")
public class AdminManageController {
    private final AdminManageService adminManageService;

    @GetMapping
    public Result list() {
        log.info("获取管理员列表");
        return adminManageService.list();
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        log.info("查询管理员详情: id={}", id);
        return adminManageService.detail(id);
    }

    @PostMapping
    public Result create(@Validated @RequestBody AdminCreateDTO dto) {
        log.info("新增管理员: username={}, role={}", dto.getUsername(), dto.getRole());
        return adminManageService.create(dto);
    }
}
