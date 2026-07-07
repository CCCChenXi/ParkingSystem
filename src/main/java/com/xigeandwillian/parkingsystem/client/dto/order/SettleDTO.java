package com.xigeandwillian.parkingsystem.client.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Valid
public class SettleDTO {
    @NotNull
    private Long lotId;
    @NotNull
    private Long spotId;
    @NotNull
    private Long seq;
    private Long couponId;
}
