package com.xigeandwillian.parkingsystem.client.vo.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderInfoVO {
    private Long id;
    private String orderNo;
    private Long lotId;
    private String lotName;
    private String spotNumber;
    private String plateNumber;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal amount;
    private BigDecimal discount;
    private LocalDateTime createTime;
}
