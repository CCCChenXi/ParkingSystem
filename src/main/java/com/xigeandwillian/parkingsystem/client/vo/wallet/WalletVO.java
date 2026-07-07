package com.xigeandwillian.parkingsystem.client.vo.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletVO implements Serializable {
    private Long id;
    private BigDecimal balance;
}
