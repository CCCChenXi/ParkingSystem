package com.xigeandwillian.parkingsystem.admin.vo.parkingspot;

import lombok.Data;

@Data
public class SpotListVO {
    private Long id;
    private String spotNumber;
    private Integer type;
    private Integer status;
}
