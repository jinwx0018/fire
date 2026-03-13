package com.fire.recommendation.exception;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(Exception e) {
        String msg = e instanceof MethodArgumentNotValidException
                ? ((MethodArgumentNotValidException) e).getBindingResult().getFieldError().getDefaultMessage()
                : ((BindException) e).getBindingResult().getFieldError().getDefaultMessage();
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), msg != null ? msg : "参数校验失败");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        log.warn("请求方法不支持: {} {} -> 该接口不支持 {} 方法", method, path, method);
        return Result.fail(405, "请求方法不支持：请使用正确的 HTTP 方法（如登录、注册、行为上报等需使用 POST）");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("", e);
        return Result.fail(ResultCode.SERVER_ERROR.getCode(), ResultCode.SERVER_ERROR.getMessage());
    }
}
