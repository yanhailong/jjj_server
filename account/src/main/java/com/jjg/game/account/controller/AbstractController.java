package com.jjg.game.account.controller;

import com.jjg.game.account.vo.WebResult;
import com.jjg.game.core.constant.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author 11
 * @date 2025/5/24 15:01
 */
public abstract class AbstractController {
    protected Logger log = LoggerFactory.getLogger(getClass());

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

    protected String genernateToken(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }
}
