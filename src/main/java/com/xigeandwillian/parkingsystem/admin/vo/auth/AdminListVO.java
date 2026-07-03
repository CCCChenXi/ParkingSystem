package com.xigeandwillian.parkingsystem.admin.vo.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminListVO {
    private Long id;
    private String username;
    private String role;
    private LocalDateTime createTime;
}
