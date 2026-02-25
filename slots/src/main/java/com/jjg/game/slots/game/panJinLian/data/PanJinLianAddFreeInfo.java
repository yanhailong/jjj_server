package com.jjg.game.slots.game.panJinLian.data;

import org.apache.poi.ss.formula.functions.T;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lihaocao
 * @date 2025/9/10 9:42
 */
public class PanJinLianAddFreeInfo {
    // 什么类型触发
    private int libType;
    // 目标图标
    private int targetIcon;

    // 详情
    private List<TimeInfo> timesInfoList;


    static class TimeInfo {
        // 出现次数
        private int times;
        // 增加次数
        private int addFreeCount;
        // 概率
        private int prop;

        public TimeInfo(int times, int addFreeCount, int prop) {
            this.prop = prop;
            this.addFreeCount = addFreeCount;
            this.times = times;
        }

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }

        public int getAddFreeCount() {
            return addFreeCount;
        }

        public void setAddFreeCount(int addFreeCount) {
            this.addFreeCount = addFreeCount;
        }

        public int getProp() {
            return prop;
        }

        public void setProp(int prop) {
            this.prop = prop;
        }
    }


    public int getLibType() {
        return libType;
    }

    public void setLibType(int libType) {
        this.libType = libType;
    }

    public int getTargetIcon() {
        return targetIcon;
    }

    public void setTargetIcon(int targetIcon) {
        this.targetIcon = targetIcon;
    }

    public List<TimeInfo> getTimesInfoList() {
        return timesInfoList;
    }

    public void setTimesInfoList(List<TimeInfo> timesInfoList) {
        this.timesInfoList = timesInfoList;
    }

    public void addTimesInfo(int times, int addFreeCount, int prop) {
        TimeInfo timeInfo = new TimeInfo(times, addFreeCount, prop);
        addTimesInfo(timeInfo);
    }

    public void addTimesInfo(TimeInfo timeInfo) {
        if (this.timesInfoList == null) {
            this.timesInfoList = new ArrayList<>();
        }
        timesInfoList.add(timeInfo);
    }

    public int getAddFreeCount(int times) {
        if (times <= 0) {
            return 0;
        }
        int bigCount = 0;
        for (TimeInfo timeInfo : timesInfoList) {
            if (timeInfo.getTimes() == times) {
                return timeInfo.getAddFreeCount();
            }
            if (times >= timeInfo.getTimes() && bigCount < timeInfo.getAddFreeCount()) {
                bigCount = timeInfo.getAddFreeCount();
            }
        }
        return bigCount;
    }
}