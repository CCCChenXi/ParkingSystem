package com.xigeandwillian.parkingsystem.admin.vo.dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {
    private Integer lotCount;
    private Integer spotCount;
    private Integer todayOrders;
    private BigDecimal todayRevenue;
    private List<TrendVO> orderTrend;
    private List<TrendVO> revenueTrend;
    private List<RecentOrderVO> recentOrders;
}
