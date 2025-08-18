package com.yifan.app_common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handleBase(BaseException e) {
        // LogUtils.error("业务异常", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception e) {
        // LogUtils.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "服务器异常"));
    }
}