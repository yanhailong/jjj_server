package com.jjg.game.gm.vo;

import com.jjg.game.activity.common.data.ActivityType;

/**
 * @author lm
 * @date 2025/9/25 13:56
 */
public class ActivityInfoVo {
    //活动期数
    private long id;
    //活动名称
    private String name;
    //活动类型(1限时活动 2开服活动)
    private int openType;
    //活动类型
    private ActivityType type;
    //活动开启时间
    private long timeStart;
    //活动结束时间
    private long timeEnd;
    //活动状态
    private int status;
    //活动开启状态
    private boolean open;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOpenType() {
        return openType;
    }

    public void setOpenType(int openType) {
        this.openType = openType;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(long timeEnd) {
        this.timeEnd = timeEnd;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
