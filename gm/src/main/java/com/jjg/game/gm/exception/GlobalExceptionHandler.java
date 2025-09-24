package com.jjg.game.gm.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lm
 * @date 2025/7/11 15:03
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    protected Logger log = LoggerFactory.getLogger(getClass());

    // 捕获自定义异常
    @ExceptionHandler(DecryptException.class)
    @ResponseBody
    public ResponseEntity<Object> handleDecryptException(DecryptException ex) {
        return ResponseEntity
                .badRequest()
                .body("验证签名失败");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String defaultMessage = error.getDefaultMessage(); // 获取默认消息
            errors.put(fieldName, defaultMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

}