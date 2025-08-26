package com.jjg.game.gm.vo;

/**
 * @author 11
 * @date 2025/5/24 17:17
 */
public class WebResult <T>{
    private int code;
    private String message;
    private T data;

    public WebResult(int code) {
        this.code = code;
    }

    public WebResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public WebResult(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public WebResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
