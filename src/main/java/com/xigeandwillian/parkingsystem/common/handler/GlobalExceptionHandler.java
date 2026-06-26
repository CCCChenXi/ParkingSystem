package com.xigeandwillian.parkingsystem.common.handler;

import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return Result.fail(ResultConstant.BAD_REQUEST, e.getMessage());

    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        return Result.fail(ResultConstant.INTERNAL_SERVER_ERROR, "服务器内部错误");
    }
}
