package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2025/8/19 11:25
 */
public class PlayerBuffDetail {
    //类型
    private int type;
    //值
    private int value;
    //过期时间
    private int expire;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }
}
