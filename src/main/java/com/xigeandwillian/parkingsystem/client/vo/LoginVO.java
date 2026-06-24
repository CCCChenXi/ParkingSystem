package com.xigeandwillian.parkingsystem.client.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private UserVO userVO;
}
