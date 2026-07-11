package com.xigeandwillian.parkingsystem.common.task;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class DashboardPreAggregationTask {

    private final ParkingOrderMapper parkingOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    @Scheduled(fixedRate = 300000)
    public void preAggregateDashboardData() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime start = today.atStartOfDay();
            LocalDateTime end = today.plusDays(1).atStartOfDay();

            LambdaQueryWrapper<ParkingOrder> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.ge(ParkingOrder::getCreateTime, start)
                    .lt(ParkingOrder::getCreateTime, end);
            int orders = parkingOrderMapper.selectCount(countWrapper).intValue();

            LambdaQueryWrapper<ParkingOrder> sumWrapper = new LambdaQueryWrapper<>();
            sumWrapper.select(ParkingOrder::getAmount)
                    .ge(ParkingOrder::getCreateTime, start)
                    .lt(ParkingOrder::getCreateTime, end);
            BigDecimal revenue = parkingOrderMapper.selectList(sumWrapper).stream()
                    .map(ParkingOrder::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            stringRedisTemplate.opsForValue().set(
                    RedisConstant.Parking.DASHBOARD_TODAY_ORDERS,
                    String.valueOf(orders),
                    RedisConstant.Parking.DASHBOARD_COUNT_TTL_DAY, TimeUnit.DAYS);
            stringRedisTemplate.opsForValue().set(
                    RedisConstant.Parking.DASHBOARD_TODAY_REVENUE,
                    revenue.toPlainString(),
                    RedisConstant.Parking.DASHBOARD_COUNT_TTL_DAY, TimeUnit.DAYS);

            String monthKey = RedisConstant.Parking.DASHBOARD_TREND + today.format(MONTH_FORMAT);
            String dateStr = today.format(DAY_FORMAT);
            String trendJson = JSONUtil.createObj()
                    .set("orders", orders)
                    .set("revenue", revenue)
                    .toString();
            stringRedisTemplate.opsForHash().put(monthKey, dateStr, trendJson);
            stringRedisTemplate.expire(monthKey, RedisConstant.Parking.DASHBOARD_TREND_TTL_DAY, TimeUnit.DAYS);

            log.debug("仪表盘数据预聚合完成: orders={}, revenue={}", orders, revenue);
        } catch (Exception e) {
            log.error("仪表盘数据预聚合失败", e);
        }
    }
}
