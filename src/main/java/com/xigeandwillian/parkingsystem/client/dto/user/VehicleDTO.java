package com.xigeandwillian.parkingsystem.client.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleDTO {
    @NotBlank(message = "车牌号不能为空")
    private String plateNumber;
    private String brand;
    private String color;
}
