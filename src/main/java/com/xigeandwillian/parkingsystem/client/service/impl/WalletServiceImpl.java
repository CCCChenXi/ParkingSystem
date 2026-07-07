package com.xigeandwillian.parkingsystem.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xigeandwillian.parkingsystem.client.mapper.WalletLogMapper;
import com.xigeandwillian.parkingsystem.client.service.service.WalletService;
import com.xigeandwillian.parkingsystem.client.vo.wallet.WalletLogVO;
import com.xigeandwillian.parkingsystem.client.vo.wallet.WalletVO;
import com.xigeandwillian.parkingsystem.common.constant.CaffeineConstant;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.Wallet;
import com.xigeandwillian.parkingsystem.common.entity.WalletLog;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.mapper.WalletMapper;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;

    private final WalletLogMapper walletLogMapper;

    /*
      钱包信息属于用户个人隐私
      1.因为属于个人信息 不宜缓存到redis
      2.为避免恶意请求或高并发 需要缓存防穿透
      3.采用Caffeine本地短时缓存策略->降低短期数据库请求次数
     */

    @Resource(name = "balanceCache")
    private Cache<String, WalletVO> balanceCache;

    /*
     钱包流水属于用户个人隐私
     */
    private final Cache<String, List<WalletLogVO>> walletLogCache = Caffeine.newBuilder()
            .maximumSize(CaffeineConstant.WALLET_MAXIMUM_SIZE)
            .expireAfterWrite(CaffeineConstant.WALLET_EXPIRE_SECONDS, TimeUnit.SECONDS)
            .build();


    /**
     * 钱包信息
     * 1.从UserHolder获取用户id
     * 2.查询钱包信息
     * @return
     */
    @Override
    public Result info() {
        Long userId = UserHolder.get();
        WalletVO walletVO = balanceCache.get(CaffeineConstant.WALLET_KEY + userId, k -> {
            log.info("钱包信息缓存未命中，查询数据库 userId: {}", userId);
            Wallet wallet = walletMapper.selectOne(new QueryWrapper<Wallet>().eq("user_id", userId));
            return wallet != null
                    ? WalletVO.builder().id(wallet.getId()).balance(wallet.getBalance()).build()
                    : WalletVO.builder().id(0L).balance(BigDecimal.ZERO).build();
        });
        return Result.ok(walletVO);
    }


    /**
     * 钱包日志
     * 1.从UserHolder获取用户id
     * 2.查wallet表获取wallet_id
     * 3.查wallet_log表按wallet_id查询最近20条，按创建时间倒序
     * 4.转为VO后缓存到Caffeine
     *
     * @return
     */
    @Override
    public Result walletLog() {
        Long userId = UserHolder.get();
        List<WalletLogVO> logs = walletLogCache.get(CaffeineConstant.WALLET_LOG_KEY + userId, k -> {
            log.info("钱包流水缓存未命中，查询数据库 userId: {}", userId);
            Wallet wallet = walletMapper.selectOne(new QueryWrapper<Wallet>().eq("user_id", userId));
            if (wallet == null) {
                return Collections.emptyList();
            }
            List<WalletLog> list = walletLogMapper.selectList(
                    new QueryWrapper<WalletLog>()
                            .eq("wallet_id", wallet.getId())
                            .orderByDesc("create_time")
                            .last("LIMIT 20"));
            List<WalletLogVO> result = new ArrayList<>(list.size());
            for (WalletLog l : list) {
                result.add(WalletLogVO.builder()
                        .id(l.getId())
                        .amount(l.getAmount())
                        .type(l.getType())
                        .remark(l.getRemark())
                        .createTime(l.getCreateTime())
                        .build());
            }
            return result;
        });
        return Result.ok(logs);
    }

    /**
     * 钱包充值
     *
     * @return
     */
    @Override
    public Result recharge(BigDecimal amount) {
        Long userId = UserHolder.get();
        //返回值0/1 表失败/成功
        int isUpdated = walletMapper.update(null,
                new UpdateWrapper<Wallet>()
                        .eq("user_id", userId)
                        .setSql("balance = balance + " + amount));
        if (isUpdated == 0) {
            //充值失败
            throw new BusinessException(ResultConstant.SYSTEM_ERROR, "服务器异常,充值失败");
        }
        //删除本地缓存
        balanceCache.invalidate(CaffeineConstant.WALLET_KEY + userId);

        //添加本地缓存
        Wallet wallet = walletMapper.selectOne(
                new QueryWrapper<Wallet>().eq("user_id", userId));
        WalletVO walletVO = WalletVO.builder()
                .id(wallet.getId())
                .balance(wallet.getBalance())
                .build();
        balanceCache.put(CaffeineConstant.WALLET_KEY + userId, walletVO);

        //记录充值流水
        WalletLog log = new WalletLog();
        log.setWalletId(wallet.getId());
        log.setAmount(amount);
        log.setType(0);
        log.setRemark("用户充值");
        walletLogMapper.insert(log);

        //失效并重建流水缓存
        String logKey = CaffeineConstant.WALLET_LOG_KEY + userId;
        walletLogCache.invalidate(logKey);
        List<WalletLog> logList = walletLogMapper.selectList(
                new QueryWrapper<WalletLog>()
                        .eq("wallet_id", wallet.getId())
                        .orderByDesc("create_time")
                        .last("LIMIT 20"));
        List<WalletLogVO> logVOs = new ArrayList<>(logList.size());
        for (WalletLog l : logList) {
            logVOs.add(WalletLogVO.builder()
                    .id(l.getId())
                    .amount(l.getAmount())
                    .type(l.getType())
                    .remark(l.getRemark())
                    .createTime(l.getCreateTime())
                    .build());
        }
        walletLogCache.put(logKey, logVOs);

        //返回余额
        return Result.ok(WalletVO.builder()
                .id(wallet.getId())
                .balance(wallet.getBalance())
                .build());
    }
}
