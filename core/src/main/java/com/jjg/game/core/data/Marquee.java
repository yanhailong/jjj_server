package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2025/8/11 19:46
 */
public class Marquee {
    private int id;
    //跑马灯内容
    private String content;
    //间隔时间
    private int interval;
    //次数
    private int nums;
    //优先级
    private int priority;
    //跑马灯类型
    private int type;
    //开始时间
    private int startTime;
    //结束时间
    private int endTime;
    //最近一次推送时间
    private int lastSendTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getNums() {
        return nums;
    }

    public void setNums(int nums) {
        this.nums = nums;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public int getLastSendTime() {
        return lastSendTime;
    }

    public void setLastSendTime(int lastSendTime) {
        this.lastSendTime = lastSendTime;
    }
}
