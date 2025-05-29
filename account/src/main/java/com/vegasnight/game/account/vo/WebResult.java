package com.vegasnight.game.account.vo;

/**
 * @author 11
 * @date 2025/5/24 17:17
 */
public class WebResult <T>{
    private int code;
    private String msg;
    private T data;

    public WebResult(int code) {
        this.code = code;
    }

    public WebResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public WebResult(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public WebResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
