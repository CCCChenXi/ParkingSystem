package com.xigeandwillian.parkingsystem.client.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorizeVO {
    private String token;
    private UserVO userVO;
}
