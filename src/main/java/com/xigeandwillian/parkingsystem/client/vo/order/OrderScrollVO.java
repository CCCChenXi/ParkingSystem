package com.xigeandwillian.parkingsystem.client.vo.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderScrollVO {
    private List<OrderInfoVO> data;
    private Long nextTimestamp;
    private Long nextId;
    private Boolean hasMore;
}
