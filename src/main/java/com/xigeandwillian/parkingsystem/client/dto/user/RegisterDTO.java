package com.xigeandwillian.parkingsystem.client.dto.user;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String phone;
    private String code;
}
