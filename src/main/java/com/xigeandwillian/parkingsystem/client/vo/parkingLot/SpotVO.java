package com.xigeandwillian.parkingsystem.client.vo.parkingLot;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpotVO {
    private Long id;
    private String spotNumber;
    private Integer type;
    private Integer status;
}
