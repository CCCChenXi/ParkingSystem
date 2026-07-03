package com.xigeandwillian.parkingsystem.admin.vo.dashboard;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrendVO {
    private String date;
    private Integer orders;
    private BigDecimal revenue;
}
