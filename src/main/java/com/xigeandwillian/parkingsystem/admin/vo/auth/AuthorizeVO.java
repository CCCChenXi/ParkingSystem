package com.xigeandwillian.parkingsystem.admin.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorizeVO {
    private String token;
    private AdminVO admin;
}
