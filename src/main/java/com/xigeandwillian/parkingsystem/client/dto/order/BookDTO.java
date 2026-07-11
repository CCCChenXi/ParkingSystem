package com.xigeandwillian.parkingsystem.client.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    @NotNull
    private Long lotId;
    @NotNull
    private Long spotId;
    @NotNull
    private Long seq;
    @NotBlank
    private String plateNumber;
}
