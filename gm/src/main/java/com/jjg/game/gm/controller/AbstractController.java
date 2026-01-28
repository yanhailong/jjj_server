package com.jjg.game.gm.controller;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.WebResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * @author 11
 * @date 2025/5/24 15:01
 */
public abstract class AbstractController {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    protected NodeManager nodeManager;
    @Autowired
    protected NodeConfig nodeConfig;

    protected <T> WebResult<T> success(T data) {
        return new WebResult<T>(Code.SUCCESS, data);
    }

    protected <T> WebResult<T> success(String msg, T data) {
        return new WebResult<T>(Code.SUCCESS, msg, data);
    }

    protected <T> WebResult<T> success(String msg) {
        return new WebResult<T>(Code.SUCCESS, msg);
    }

    protected <T> WebResult<T> fail(int code) {
        return new WebResult<T>(code);
    }

    protected <T> WebResult<T> fail() {
        return new WebResult<T>(Code.FAIL);
    }

    protected <T> WebResult<T> fail(String msg) {
        return new WebResult<T>(Code.FAIL, msg);
    }

    protected <T> WebResult<T> fail(T data) {
        return new WebResult<T>(Code.FAIL, data);
    }

    protected <T> WebResult<T> exception() {
        return new WebResult<T>(Code.EXCEPTION);
    }

    protected String genernateToken() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
