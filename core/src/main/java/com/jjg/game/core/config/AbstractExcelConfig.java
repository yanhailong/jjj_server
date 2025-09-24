package com.jjg.game.core.config;

import com.alibaba.fastjson.JSON;

/**
 * excel配置抽象类
 */
public abstract class AbstractExcelConfig {

    /**
     * 唯一id
     */
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toJSONString() {
        return JSON.toJSONString(this);
    }
}
