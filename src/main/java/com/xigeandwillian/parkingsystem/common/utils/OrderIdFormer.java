package com.xigeandwillian.parkingsystem.common.utils;

import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class OrderIdFormer {

    private final StringRedisTemplate stringRedisTemplate;

    /*订单计算开始时间戳*/
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    /*序列号位数*/
    private static final int COUNT_BITS = 32;

    public String nextId(String prefix){
        //获取当前时间戳
        LocalDateTime now = LocalDateTime.now();
        //转秒
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        //获取当前时间序列号
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;

        //获取今天日期
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));

        long count = stringRedisTemplate.opsForValue().increment(RedisConstant.Order.ORDER_ID_PREFIX+prefix+":"+date);

        return String.valueOf(timeStamp << COUNT_BITS | count);

    }

}
