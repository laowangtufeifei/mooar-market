package com.mooar.market.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Result<?> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 路径/查询参数类型不对，例如把合约地址传给了要求数字的参数
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型错误 param={} value={}", e.getName(), e.getValue());
        return Result.fail(ErrorCode.PARAM_ERROR);
    }

    /**
     * 缺少必填参数、请求体格式错误
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public Result<?> handleBadRequest(Exception e) {
        log.warn("请求参数错误 err={}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("unexpected error", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }
}
