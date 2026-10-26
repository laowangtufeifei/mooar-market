package com.mooar.market.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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

    /**
     * 请求方法不对（如 GET 接口用 POST 调）。属于客户端用法问题，只记一行 warn，不打栈
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                              HttpServletRequest request) {
        log.warn("请求方法不支持 uri={} method={} supported={}",
                request.getRequestURI(), e.getMethod(), e.getSupportedHttpMethods());
        return Result.fail(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * Content-Type 不对（如 POST JSON 接口没带 application/json）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<?> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e,
                                                 HttpServletRequest request) {
        log.warn("Content-Type 不支持 uri={} contentType={}", request.getRequestURI(), e.getContentType());
        return Result.fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("unexpected error", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }
}
