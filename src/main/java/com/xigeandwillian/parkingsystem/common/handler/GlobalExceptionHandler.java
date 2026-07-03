package com.xigeandwillian.parkingsystem.common.handler;

import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e){
        log.warn(e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        log.warn(msg);
        return Result.fail(ResultConstant.BAD_REQUEST, msg);
    }
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("异常: {}", e.getMessage());
        return Result.fail(ResultConstant.INTERNAL_SERVER_ERROR, "服务器内部错误");
    }
}
