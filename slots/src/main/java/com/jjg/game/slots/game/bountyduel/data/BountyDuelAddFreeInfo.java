package com.jjg.game.slots.game.bountyduel.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 免费模式重触发配置。
 */
public class BountyDuelAddFreeInfo {
    /**
     * 触发模式类型。
     */
    private int libType;
    /**
     * 目标图标。
     */
    private int targetIcon;
    /**
     * 图标出现次数对应的加免费次数配置。
     */
    private List<TimeInfo> timesInfoList;

    static class TimeInfo {
        /**
         * 出现次数。
         */
        private int times;
        /**
         * 增加免费次数。
         */
        private int addFreeCount;
        /**
         * 权重。
         */
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
