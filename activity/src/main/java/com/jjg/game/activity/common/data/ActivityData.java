package com.jjg.game.activity.common.data;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.sampledata.bean.ActivityConfigCfg;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/4 18:02
 */
public class ActivityData {
    /**
     * 活动id
     */
    private long id;

    /**
     * 活动状态 1未开启 2进行中 3已结束
     */
    private int status = 1;
    /**
     * 开服活动轮数
     */
    private int round;

    /**
     * 解锁条件
     */
    private Map<Integer, Integer> condition;
    /**
     * 活动持续时间(天)
     */
    private int duration;
    /**
     * 跑马灯通知
     */
    private int marquee;
    /**
     * 活动名称
     */
    private int name;
    /**
     * 是否开启
     */
    private boolean open;
    /**
     * 活动开启方式(1开服、2限时)
     */
    private int openType;
    /**
     * 结束时间
     */
    private long timeEnd;
    /**
     * 开始时间
     */
    private long timeStart;
    /**
     * 类型ID
     */
    private ActivityType type;
    /**
     * 值1
     */
    private List<Integer> value;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Map<Integer, Integer> getCondition() {
        return condition;
    }

    public void setCondition(Map<Integer, Integer> condition) {
        this.condition = condition;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getMarquee() {
        return marquee;
    }

    public void setMarquee(int marquee) {
        this.marquee = marquee;
    }

    public int getName() {
        return name;
    }

    public void setName(int name) {
        this.name = name;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getOpenType() {
        return openType;
    }

    public void setOpenType(int openType) {
        this.openType = openType;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(long timeEnd) {
        this.timeEnd = timeEnd;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public List<Integer> getValue() {
        return value;
    }

    public void setValue(List<Integer> value) {
        this.value = value;
    }

    public void addRound() {
        this.round++;
    }

    public boolean canRun() {
        return isOpen() && status == ActivityConstant.ActivityStatus.RUNNING;
    }

    public static ActivityData getActivityData(ActivityConfigCfg cfg, ActivityType activityType) {
        if (cfg == null) {
            return null;
        }
        ActivityData data = new ActivityData();
        data.setId(cfg.getId());

        data.setCondition(cfg.getCondition());
        data.setDuration(cfg.getDuration());
        data.setMarquee(cfg.getMarquee());
        data.setName(cfg.getName());
        data.setOpen(cfg.getOpen());
        data.setOpenType(cfg.getOpen_type());
        data.setType(activityType);
        data.setValue(cfg.getValue());

        // 解析时间字符串为时间戳（假设格式是 yyyy-MM-dd HH:mm:ss）
        if (StringUtils.isNotEmpty(cfg.getTime_start())) {
            data.setTimeStart(TimeHelper.getTimestamp(cfg.getTime_start()));
        }

        if (StringUtils.isNotEmpty(cfg.getTime_end())) {
            data.setTimeEnd(TimeHelper.getTimestamp(cfg.getTime_end()));
        }
        return data;
    }
}
