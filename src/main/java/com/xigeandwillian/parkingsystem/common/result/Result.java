package com.xigeandwillian.parkingsystem.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.xigeandwillian.parkingsystem.common.constant.ResultConstant.SUCCESS;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS, null, data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(SUCCESS, null, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
