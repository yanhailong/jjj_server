package com.jjg.game.gm.vo;

/**
 * @author 11
 * @date 2025/8/29 17:48
 */
public class PageVo<T> {
    private long count;
    private T data;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
