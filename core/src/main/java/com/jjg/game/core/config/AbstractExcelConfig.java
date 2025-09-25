package com.jjg.game.core.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;

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

    public String getMd5() {
        return DigestUtils.md5Hex(toJSONString());
    }

}
