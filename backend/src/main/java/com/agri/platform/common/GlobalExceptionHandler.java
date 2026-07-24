package com.agri.platform.common;

import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * 将 Service 抛出的 RuntimeException 等统一包装为 Result 返回，
 * 避免前端拿到 Spring 默认的错误 JSON 而无法解析 code/message。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（Service 主动抛出，带中文提示） */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }

    /** 缺少请求头（如未携带 Authorization） */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public Result<Void> handleMissingHeader(MissingRequestHeaderException e) {
        return Result.error(401, "未登录或登录已过期");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error("服务器异常：" + e.getMessage());
    }
}
