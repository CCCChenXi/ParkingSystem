package com.xigeandwillian.parkingsystem.admin.controller;

import com.xigeandwillian.parkingsystem.admin.service.Service.DashboardService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public Result dashboard() {
        log.info("获取仪表盘数据");
        return dashboardService.dashboard();
    }
}
