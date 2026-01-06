package com.jjg.game.slots.data;

public class OffLineEventData {
    //事件id
    private int id;
    //该事件执行时间
    private long actionMills;
    //离线后延迟执行时间
    private int delayMills;
    //是否已经执行
    private boolean action;

    public OffLineEventData(int id) {
        this.id = id;
    }

    public OffLineEventData(int id, long actionMills) {
        this.id = id;
        this.actionMills = actionMills;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getActionMills() {
        return actionMills;
    }

    public void setActionMills(long actionMills) {
        this.actionMills = actionMills;
    }

    public int getDelayMills() {
        return delayMills;
    }

    public void setDelayMills(int delayMills) {
        this.delayMills = delayMills;
    }

    public boolean isAction() {
        return action;
    }

    public void setAction(boolean action) {
        this.action = action;
    }
}
