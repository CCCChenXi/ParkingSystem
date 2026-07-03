package com.xigeandwillian.parkingsystem.admin.vo.user;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminUserListVO {
    private Long id;
    private String username;
    private String phone;
    private Integer vehicles;
    private Integer orderCount;
    private BigDecimal balance;
    private LocalDateTime createTime;
}
