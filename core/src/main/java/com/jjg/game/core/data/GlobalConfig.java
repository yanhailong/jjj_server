package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lm
 * @date 2026/4/24 10:19
 */
@Document
public class GlobalConfig {
    @Id
    private int id;
    private String value;
    private int intValue;
    private long longValue;
    private boolean boolValue;

    public GlobalConfig() {
    }

    public GlobalConfig(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public boolean isBoolValue() {
        return boolValue;
    }

    public void setBoolValue(boolean boolValue) {
        this.boolValue = boolValue;
    }
}
