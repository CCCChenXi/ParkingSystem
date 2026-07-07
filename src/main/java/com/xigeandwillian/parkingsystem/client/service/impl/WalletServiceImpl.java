package com.xigeandwillian.parkingsystem.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.xigeandwillian.parkingsystem.client.service.service.WalletService;
import com.xigeandwillian.parkingsystem.common.constant.CaffeineConstant;
import com.xigeandwillian.parkingsystem.common.entity.Wallet;
import com.xigeandwillian.parkingsystem.common.mapper.WalletMapper;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;

    /*
      钱包信息属于用户个人隐私
      1.因为属于个人信息 不宜缓存到redis
      2.为避免恶意请求或高并发 需要缓存防穿透
      3.采用Caffeine本地短时缓存策略->降低短期数据库请求次数
     */

    @Resource(name = "walletBalanceCache")
    private Cache<String, BigDecimal> balanceCache;



    /**
     * 钱包信息
     * 1.从UserHolder获取用户id
     * 2.查询钱包信息
     * @return
     */
    @Override
    public Result info() {
        Long userId = UserHolder.get();
        BigDecimal balance = balanceCache.get(CaffeineConstant.WALLET_KEY + userId, k -> {
            log.info("钱包信息缓存未命中，查询数据库 userId: {}", userId);
            Wallet wallet = walletMapper.selectOne(new QueryWrapper<Wallet>().eq("user_id", userId));
            return wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
        });
        return Result.ok(balance);
    }


    /**
     * 钱包日志
     * @return
     */
    @Override
    public Result walletLog() {
        return null;
    }
}
