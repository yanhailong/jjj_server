package com.jjg.game.activity.common.data;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.util.CronUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.sampledata.bean.ActivityConfigCfg;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
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
    private long round;

    /**
     * 解锁条件
     */
    private String condition;
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

    /**
     * 值2
     */
    private List<Long> valueParam;

    /**
     * bigDecimal参数
     */
    private List<BigDecimal> bigDecimalParam;
    /**
     * 渠道和商品ID
     */
    private Map<Integer, String> channelCommodity;
    /**
     * 道具掉落包ID
     */
    private Integer dropId;

    /**
     * 执行的开始corn表达式
     */
    private String timeStartCorn;

    /**
     * 执行的结束corn表达式
     */
    private String timeEndCorn;

    public String getTimeStartCorn() {
        return timeStartCorn;
    }

    public void setTimeStartCorn(String timeStartCorn) {
        this.timeStartCorn = timeStartCorn;
    }

    public String getTimeEndCorn() {
        return timeEndCorn;
    }

    public void setTimeEndCorn(String timeEndCorn) {
        this.timeEndCorn = timeEndCorn;
    }

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

    public Map<Integer, String> getChannelCommodity() {
        return channelCommodity;
    }

    public void setChannelCommodity(Map<Integer, String> channelCommodity) {
        this.channelCommodity = channelCommodity;
    }

    public void setRound(long round) {
        this.round = round;
    }

    public long getRound() {
        return round;
    }

    public List<Long> getValueParam() {
        return valueParam;
    }

    public void setValueParam(List<Long> valueParam) {
        this.valueParam = valueParam;
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

    public Integer getDropId() {
        return dropId;
    }

    public void setDropId(Integer dropId) {
        this.dropId = dropId;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public List<BigDecimal> getBigDecimalParam() {
        return bigDecimalParam;
    }

    public void setBigDecimalParam(List<BigDecimal> bigDecimalParam) {
        this.bigDecimalParam = bigDecimalParam;
    }

    public boolean canRun() {
        return isOpen() && status == ActivityConstant.ActivityStatus.RUNNING;
    }

    public static ActivityData getActivityData(ActivityConfigCfg cfg, ActivityType activityType, long startServerTime) {
        if (cfg == null) {
            return null;
        }
        ActivityData data = new ActivityData();
        data.setId(cfg.getId());
        data.setRound(cfg.getId());
        data.setCondition(cfg.getCondition());
        data.setDuration(cfg.getDuration());
        data.setMarquee(cfg.getMarquee());
        data.setName(cfg.getName());
        data.setOpen(cfg.getOpen());
        data.setOpenType(cfg.getOpen_type());
        data.setType(activityType);
        data.setValue(cfg.getValue());
        data.setValueParam(cfg.getValueParam());
        data.setDropId(cfg.getDropConfigId());
        data.setBigDecimalParam(cfg.getBigDecimalParam());
        data.setChannelCommodity(cfg.getChannelCommodity());
        long currentTimeMillis = System.currentTimeMillis();
        switch (data.getOpenType()) {
            case ActivityConstant.Common.CYCLE_SERVER_TYPE -> {
                //循环设置cron
                data.setTimeStartCorn(cfg.getTime_start());
                data.setTimeEndCorn(cfg.getTime_end());
                LocalDateTime offset = LocalDateTime.now().minusMonths(1);
                if (activityType == ActivityType.OFFICIAL_AWARDS) {
                    offset = offset.with(TemporalAdjusters.lastDayOfMonth());
                }
                do {
                    //获取下次执行时间
                    Pair<LocalDateTime, LocalDateTime> nextOpenTime = CronUtil.getNextOpenTime(data.getTimeStartCorn(), data.getTimeEndCorn(), offset);
                    if (nextOpenTime != null) {
                        data.setTimeStart(TimeHelper.getTimestamp(nextOpenTime.getFirst()));
                        data.setTimeEnd(TimeHelper.getTimestamp(nextOpenTime.getSecond()));
                        offset = nextOpenTime.getSecond();
                    }
                } while (currentTimeMillis >= data.getTimeEnd());
            }
            case ActivityConstant.Common.LIMIT_TYPE -> {
                // 解析时间字符串为时间戳（假设格式是 yyyy-MM-dd HH:mm:ss）
                if (StringUtils.isNotEmpty(cfg.getTime_start())) {
                    data.setTimeStart(TimeHelper.getTimestamp(cfg.getTime_start().trim()));
                }
                // 解析时间字符串为时间戳（假设格式是 yyyy-MM-dd HH:mm:ss）
                if (StringUtils.isNotEmpty(cfg.getTime_end())) {
                    data.setTimeEnd(TimeHelper.getTimestamp(cfg.getTime_end().trim()));
                }
            }
            case ActivityConstant.Common.OPEN_SERVER_TYPE -> {
                //开服 开始时间戳为0设置为开服时间，结束时间戳为持续时间的时间戳 添加结束时间
                data.setTimeEnd(startServerTime);
                data.setTimeStart(startServerTime);
                if (data.getTimeEnd() < currentTimeMillis) {
                    do {
                        long timestampByDay = TimeHelper.getTimestampByDay(data.getTimeStart(), data.getDuration());
                        data.setTimeStart(data.getTimeEnd());
                        data.setTimeEnd(timestampByDay);
                    } while (currentTimeMillis >= data.getTimeEnd());
                }
            }
        }
        return data;
    }
}
