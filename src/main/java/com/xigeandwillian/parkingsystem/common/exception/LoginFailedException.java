package com.xigeandwillian.parkingsystem.common.exception;

public class LoginFailedException extends RuntimeException {
    private Integer code;

    public LoginFailedException(String message) {
        super(message);
    }
    public LoginFailedException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode(){
        return code;
    }
}
