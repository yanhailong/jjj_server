package com.jjg.game.activity.cashcow.data;

/**
 * 摇钱树记录
 *
 * @author lm
 * @date 2025/9/9 17:19
 */
public class CashCowRecordData {
    //期数
    private long round;
    //时间
    private long recordTime;
    //昵称
    private String name;
    //类型
    private int type;
    //获奖数量
    private long num;

    public CashCowRecordData() {
    }

    public CashCowRecordData(long round, long recordTime, String name, int type, long num) {
        this.round = round;
        this.recordTime = recordTime;
        this.name = name;
        this.type = type;
        this.num = num;
    }

    public long getRound() {
        return round;
    }

    public void setRound(long round) {
        this.round = round;
    }

    public long getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(long recordTime) {
        this.recordTime = recordTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getNum() {
        return num;
    }

    public void setNum(long num) {
        this.num = num;
    }
}
