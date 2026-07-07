package com.xigeandwillian.parkingsystem.client.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private Long lotId;
    private Long spotId;
    private Long seq;
    private String plateNumber;
}
