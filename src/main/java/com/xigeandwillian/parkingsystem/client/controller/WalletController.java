package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.service.service.WalletService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 获取钱包流水
     *
     * @return
     */
    @GetMapping("/log")
    public Result walletLog(){
        log.info("获取钱包流水");
        return walletService.walletLog();
    }

}
