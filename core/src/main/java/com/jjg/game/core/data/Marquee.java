package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2025/8/11 19:46
 */
public class Marquee {
    private int id;
    //跑马灯内容
    private LanguageData content;
    //播放时间
    private int showTime;
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
    //创建时间
    private int createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LanguageData getContent() {
        return content;
    }

    public void setContent(LanguageData content) {
        this.content = content;
    }

    public int getShowTime() {
        return showTime;
    }

    public void setShowTime(int showTime) {
        this.showTime = showTime;
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

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }
}
