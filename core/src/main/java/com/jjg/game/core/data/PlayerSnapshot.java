package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.Map;

/**
 * @author lm
 * @date 2026/1/22 14:44
 */
public class PlayerSnapshot {
    @Id
    private long uid;
    private Map<String, String> keys;
    private Date updateTime;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}

