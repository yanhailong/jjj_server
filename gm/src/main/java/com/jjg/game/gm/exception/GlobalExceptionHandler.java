package com.jjg.game.gm.exception;

import com.jjg.game.core.constant.Code;
import com.jjg.game.gm.vo.WebResult;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author lm
 * @date 2025/7/11 15:03
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // 捕获自定义异常
    @ExceptionHandler(DecryptException.class)
    @ResponseBody
    public ResponseEntity<Object> handleDecryptException(DecryptException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new WebResult<T>(Code.FAIL, "参数错误"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new WebResult<T>(Code.FAIL, ex.getMessage()));
    }

}