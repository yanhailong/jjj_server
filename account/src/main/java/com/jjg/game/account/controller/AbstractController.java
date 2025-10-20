package com.jjg.game.account.controller;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.WebResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 11
 * @date 2025/5/24 15:01
 */
public abstract class AbstractController {
    protected Logger log = LoggerFactory.getLogger(getClass());

    protected <T> WebResult success(){
        return new WebResult<T>(Code.SUCCESS);
    }
    protected <T> WebResult success(T data){
        return new WebResult<T>(Code.SUCCESS,data);
    }

    protected <T> WebResult fail(int code){
        return new WebResult<T>(code);
    }
    protected <T> WebResult fail(){
        return new WebResult<T>(Code.FAIL);
    }

    protected <T> WebResult fail(String msg){
        return new WebResult<T>(Code.FAIL,msg);
    }

    protected <T> WebResult fail(T data){
        return new WebResult<T>(Code.FAIL,data);
    }

    protected <T> WebResult exception(){
        return new WebResult<T>(Code.EXCEPTION);
    }

    protected String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}
