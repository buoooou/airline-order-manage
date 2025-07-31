package com.postion.airlineorderbackend.exception;

import com.postion.airlineorderbackend.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        log.error("业务异常: {}", ex.getMessage(), ex);
        ApiResponse<Object> apiResponse = ApiResponse.error(ex.getStatus().value(), ex.getMessage());
        return new ResponseEntity<>(apiResponse, ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        log.error("未捕获的系统异常: {}", ex.getMessage(), ex);
        ApiResponse<Object> apiResponse = ApiResponse.error(500, "服务器内部错误，请联系管理员");
        return new ResponseEntity<>(apiResponse, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }
}