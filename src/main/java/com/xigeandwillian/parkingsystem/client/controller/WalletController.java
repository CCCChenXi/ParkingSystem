package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.service.service.WalletService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    /**
     * 获取钱包信息
     *
     * @return
     */
    @GetMapping
    public Result walletInfo() {
        log.info("获取钱包信息");
        return walletService.info();
    }

    /**
     * 钱包充值
     *
     * @return
     */
    @PostMapping("/recharge")
    public Result recharge(@RequestParam BigDecimal amount) {
        return walletService.recharge(amount);
    }


    /**
     * 获取钱包流水
     *
     * @return
     */
    @GetMapping("/logs")
    public Result walletLog() {
        log.info("获取钱包流水");
        return walletService.walletLog();
    }

}
