package com.xigeandwillian.parkingsystem.common.exception;

public class RegisterFailedException extends RuntimeException {
    private Integer code;

    public RegisterFailedException(String message) {
        super(message);
    }

    public RegisterFailedException(Integer code, String message) {
        super(message);
    }

    public Integer getCode() {
        return code;
    }
}
