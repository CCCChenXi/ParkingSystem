package com.xigeandwillian.parkingsystem.client.vo.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class OrderConsumeVO {
    private Long orderId;
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal payable;
    private BigDecimal balance;
}
