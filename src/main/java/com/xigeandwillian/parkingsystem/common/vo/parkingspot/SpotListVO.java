package com.xigeandwillian.parkingsystem.common.vo.parkingspot;

import lombok.Data;

@Data
public class SpotListVO {
    private Long id;
    private Long seq;
    private String spotNumber;
    private Integer type;
    private Integer status;
}
