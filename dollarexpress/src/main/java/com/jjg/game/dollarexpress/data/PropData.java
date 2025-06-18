package com.jjg.game.dollarexpress.data;

/**
 * @author 11
 * @date 2025/6/16 10:45
 */
public class PropData<T> {
    private T key;
    private int begin;
    private int end;

    public PropData() {
    }

    public PropData(T key, int begin, int end) {
        this.key = key;
        this.begin = begin;
        this.end = end;
    }

    public T getKey() {
        return key;
    }

    public void setKey(T key) {
        this.key = key;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}
