package com.xigeandwillian.parkingsystem.admin.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.admin.service.Service.DashboardService;
import com.xigeandwillian.parkingsystem.admin.vo.dashboard.DashboardVO;
import com.xigeandwillian.parkingsystem.admin.vo.dashboard.RecentOrderVO;
import com.xigeandwillian.parkingsystem.admin.vo.dashboard.TrendVO;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingOrderMapper;
import com.xigeandwillian.parkingsystem.common.mapper.ParkingSpotMapper;
import com.xigeandwillian.parkingsystem.common.constant.OrderConstant;
import com.xigeandwillian.parkingsystem.common.constant.RedisConstant;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.entity.ParkingOrder;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class DashboardServiceImpl implements DashboardService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ParkingLotMapper parkingLotMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final ParkingOrderMapper parkingOrderMapper;

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    public Result dashboard() {
        DashboardVO vo = new DashboardVO();

        vo.setLotCount(getLotCount());
        vo.setSpotCount(getSpotCount());

        LocalDate today = LocalDate.now();
        vo.setTodayOrders(getTodayOrders(today));
        vo.setTodayRevenue(getTodayRevenue(today));

        List<TrendVO> trends = getTrends(today);
        vo.setOrderTrend(trends.stream()
                .map(t -> {
                    TrendVO o = new TrendVO();
                    o.setDate(t.getDate());
                    o.setOrders(t.getOrders());
                    return o;
                }).collect(Collectors.toList()));
        vo.setRevenueTrend(trends.stream()
                .map(t -> {
                    TrendVO r = new TrendVO();
                    r.setDate(t.getDate());
                    r.setRevenue(t.getRevenue());
                    return r;
                }).collect(Collectors.toList()));

        vo.setRecentOrders(getRecentOrders());

        return Result.ok(vo);
    }

    private Integer getLotCount() {
        try {
            String val = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.Parking.DASHBOARD_LOT_COUNT);
            if (val != null) {
                return Integer.parseInt(val);
            }
        } catch (Exception e) {
            log.warn("从缓存获取停车场总数失败，降级数据库", e);
        }
        Long count = parkingLotMapper.selectCount(Wrappers.emptyWrapper());
        try {
            stringRedisTemplate.opsForValue()
                    .set(RedisConstant.Parking.DASHBOARD_LOT_COUNT, String.valueOf(count),
                            RedisConstant.Parking.DASHBOARD_COUNT_TTL_DAY, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("写入停车场总数缓存失败", e);
        }
        return count.intValue();
    }

    private Integer getSpotCount() {
        try {
            String val = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.Parking.DASHBOARD_SPOT_COUNT);
            if (val != null) {
                return Integer.parseInt(val);
            }
        } catch (Exception e) {
            log.warn("从缓存获取车位总数失败，降级数据库", e);
        }
        Long count = parkingSpotMapper.selectCount(Wrappers.emptyWrapper());
        try {
            stringRedisTemplate.opsForValue()
                    .set(RedisConstant.Parking.DASHBOARD_SPOT_COUNT, String.valueOf(count),
                            RedisConstant.Parking.DASHBOARD_COUNT_TTL_DAY, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("写入车位总数缓存失败", e);
        }
        return count.intValue();
    }

    private Integer getTodayOrders(LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        LambdaQueryWrapper<ParkingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ParkingOrder::getCreateTime, start)
                .lt(ParkingOrder::getCreateTime, end);
        return parkingOrderMapper.selectCount(wrapper).intValue();
    }

    private BigDecimal getTodayRevenue(LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        LambdaQueryWrapper<ParkingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ParkingOrder::getAmount)
                .ge(ParkingOrder::getCreateTime, start)
                .lt(ParkingOrder::getCreateTime, end);
        List<ParkingOrder> orders = parkingOrderMapper.selectList(wrapper);
        return orders.stream()
                .map(ParkingOrder::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<TrendVO> getTrends(LocalDate today) {
        List<TrendVO> result = new ArrayList<>();
        LocalDate startDate = today.minusDays(6);

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateStr = date.format(DAY_FORMAT);

            if (date.equals(today)) {
                TrendVO vo = getTrendFromDb(date, dateStr);
                result.add(vo);
            } else {
                TrendVO vo = getPastDayTrend(date, dateStr);
                result.add(vo);
            }
        }
        return result;
    }

    private TrendVO getPastDayTrend(LocalDate date, String dateStr) {
        String monthKey = RedisConstant.Parking.DASHBOARD_TREND + date.format(MONTH_FORMAT);
        try {
            Object cached = stringRedisTemplate.opsForHash().get(monthKey, dateStr);
            if (cached != null) {
                String json = cached.toString();
                JSONObject obj = JSONUtil.parseObj(json);
                TrendVO vo = new TrendVO();
                vo.setDate(dateStr);
                vo.setOrders(obj.getInt("orders", 0));
                vo.setRevenue(obj.getBigDecimal("revenue", BigDecimal.ZERO));
                return vo;
            }
        } catch (Exception e) {
            log.warn("从缓存获取趋势数据失败，降级数据库: date={}", dateStr, e);
        }

        TrendVO vo = getTrendFromDb(date, dateStr);
        try {
            JSONObject obj = JSONUtil.createObj()
                    .set("orders", vo.getOrders())
                    .set("revenue", vo.getRevenue());
            stringRedisTemplate.opsForHash().put(monthKey, dateStr, obj.toString());
            stringRedisTemplate.expire(monthKey, RedisConstant.Parking.DASHBOARD_TREND_TTL_DAY, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("写入趋势缓存失败: date={}", dateStr, e);
        }
        return vo;
    }

    private TrendVO getTrendFromDb(LocalDate date, String dateStr) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

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

        TrendVO vo = new TrendVO();
        vo.setDate(dateStr);
        vo.setOrders(orders);
        vo.setRevenue(revenue);
        return vo;
    }

    private List<RecentOrderVO> getRecentOrders() {
        LambdaQueryWrapper<ParkingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ParkingOrder::getCreateTime)
                .last("LIMIT 10");
        List<ParkingOrder> orders = parkingOrderMapper.selectList(wrapper);

        Map<Long, String> lotNameMap = new HashMap<>();
        if (!orders.isEmpty()) {
            List<Long> lotIds = orders.stream()
                    .map(ParkingOrder::getLotId)
                    .distinct()
                    .collect(Collectors.toList());
            List<ParkingLot> lots = parkingLotMapper.selectBatchIds(lotIds);
            lotNameMap = lots.stream()
                    .collect(Collectors.toMap(ParkingLot::getId, ParkingLot::getName));
        }

        Map<Long, String> finalLotNameMap = lotNameMap;
        return orders.stream().map(order -> {
            RecentOrderVO vo = new RecentOrderVO();
            vo.setLotName(finalLotNameMap.getOrDefault(order.getLotId(), ""));
            vo.setPlate(order.getPlateNumber());
            vo.setStatus(formatStatus(order.getStatus()));
            return vo;
        }).collect(Collectors.toList());
    }

    private String formatStatus(Integer status) {
        if (status == null) return "";
        switch (status) {
            case OrderConstant.ORDER_STATUS_RESERVED:
                return "已预约";
            case OrderConstant.ORDER_STATUS_IN_PROGRESS:
                return "进行中";
            case OrderConstant.ORDER_STATUS_SETTLED:
                return "已结算";
            case OrderConstant.ORDER_STATUS_CANCELLED:
                return "已取消";
            default:
                return "";
        }
    }
}
