package com.xigeandwillian.parkingsystem.client.service.service;

import com.xigeandwillian.parkingsystem.common.result.Result;

import java.math.BigDecimal;

public interface WalletService {

    Result info();

    Result walletLog();

    Result recharge(BigDecimal amount);

}
