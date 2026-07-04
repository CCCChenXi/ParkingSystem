package com.xigeandwillian.parkingsystem.client.vo.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletLogVO implements Serializable {
    private Long id;
    private BigDecimal amount;
    private Integer type;
    private String remark;
    private LocalDateTime createTime;
}
