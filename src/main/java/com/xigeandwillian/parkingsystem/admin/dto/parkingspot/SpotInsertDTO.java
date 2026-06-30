package com.xigeandwillian.parkingsystem.admin.dto.parkingspot;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SpotInsertDTO {

    @NotNull(message = "停车场ID不能为空")
    private Long lotId;

    @NotNull(message = "车位编号不能为空")
    private List<String> spotNumbers;

    @NotNull(message = "车位类型不能为空")
    private Integer type;
}
