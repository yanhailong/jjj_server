package com.jjg.game.core.constant;

/**
 * @author 11
 * @date 2025/5/26 11:34
 */
public interface Code {
    //成功
    int SUCCESS = 200;
    //失败
    int FAIL = 400;
    //错误的请求
    int ERROR_REQ = 401;
    //参数错误
    int PARAM_ERROR = 402;
    //已存在
    int EXIST = 403;
    //未找到
    int NOT_FOUND = 404;
    //禁止
    int FORBID = 405;
    //重复操作
    int REPEAT_OP = 406;
    //过期
    int EXPIRE = 407;
    //余额不足
    int NOT_ENOUGHT = 408;


    //服务器错误
    int EXCEPTION = 500;
}
