package com.jjg.game.dollarexpress.data;

/**
 * @author 11
 * @date 2025/6/16 10:45
 */
public class PropData {
    private int key;
    private int begin;
    private int end;

    public PropData() {
    }

    public PropData(int key, int begin, int end) {
        this.key = key;
        this.begin = begin;
        this.end = end;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
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
