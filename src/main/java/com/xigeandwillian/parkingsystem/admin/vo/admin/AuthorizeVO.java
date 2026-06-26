package com.xigeandwillian.parkingsystem.admin.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorizeVO {
    private String token;
    private AdminVO admin;
}
