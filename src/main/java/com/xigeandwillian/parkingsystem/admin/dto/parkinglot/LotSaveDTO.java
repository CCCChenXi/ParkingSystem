package com.xigeandwillian.parkingsystem.admin.dto.parkinglot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LotSaveDTO {
    private Long id;

    @NotBlank(message = "停车场名称不能为空")
    private String name;

    @NotBlank(message = "地址不能为空")
    private String address;

    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;

    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
