package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * 玩家 sim 基础数据
 *
 * @author 11
 * @date 2026/5/15
 */
@Document
public class SimBaseData extends AbstractData {
    @Id
    private long playerId;
    //体力值
    private int stamina;
    //当前所在场景id
    private int currentCasinoId;
    //是否已完成新手引导
    private boolean guide;
    private Set<Integer> triggeredGuideGroupIds;
    private Set<Integer> completedGuideGroupIds;
    // 已完成的具体引导步骤ID，用于断线后恢复组内进度
    private Set<Integer> completedGuideIds;
    //上次离线时间 (ms), 用于长/短时掉线判定
    private long lastOfflineTime;
    //能量值
    private int power;
    //每日掉落次数 (dropItemId -> 当日已掉次数)
    private Map<Integer, Integer> dailyDropCount;
    //每日掉落计数重置日 (yyyyMMdd)
    private int dropResetDay;
    //所有场景等级之和
    private int allLevel;
    //经营信息-高级游客人次 (玩家跨娱乐城累计)
    private long receptionCount;
    //经营信息-经营总收益 (玩家跨娱乐城累计金币)
    private long businessIncome;
    //经营信息-观看广告数 (玩家跨娱乐城累计)
    private int watchAdCount;
    //经营信息-完成任务数 (玩家跨娱乐城累计, 完成即计数)
    private int finishedTaskCount;
    //最近一次触发"登陆天数"条件的自然日 (yyyyMMdd); 每个自然日仅计一次登陆, 跨天再计
    private int lastLoginDay;
    //经营信息-SPINE游戏统计 gameType -> 玩家累计统计
    private Map<Integer, SlotGameStatsData> slotStatsMap;
    //旧版按娱乐城保存的统计是否已迁移到玩家数据
    private boolean operationStatsMigrated;
    //所有的激活的勋章
    private Set<Integer> allMedalIds;
    //旧背包勋章是否已迁移到 allMedalIds
    private boolean medalDataMigrated;
    //展示的勋章
    private List<Integer> showMedalIds;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public boolean isGuide() {
        return guide;
    }

    public void setGuide(boolean guide) {
        this.guide = guide;
    }

    public Set<Integer> getTriggeredGuideGroupIds() {
        if (triggeredGuideGroupIds == null) triggeredGuideGroupIds = new HashSet<>();
        return triggeredGuideGroupIds;
    }

    public void setTriggeredGuideGroupIds(Set<Integer> value) {
        this.triggeredGuideGroupIds = value;
    }

    public Set<Integer> getCompletedGuideGroupIds() {
        if (completedGuideGroupIds == null) completedGuideGroupIds = new HashSet<>();
        return completedGuideGroupIds;
    }

    public void setCompletedGuideGroupIds(Set<Integer> value) {
        this.completedGuideGroupIds = value;
    }

    public Set<Integer> getCompletedGuideIds() {
        if (completedGuideIds == null) completedGuideIds = new HashSet<>();
        return completedGuideIds;
    }

    public void setCompletedGuideIds(Set<Integer> value) {
        this.completedGuideIds = value;
    }

    public boolean completeGuideId(int guideId) {
        return guideId > 0 && getCompletedGuideIds().add(guideId);
    }

    public List<Integer> completedGuideIds() {
        List<Integer> result = new ArrayList<>(getCompletedGuideIds());
        result.sort(Integer::compareTo);
        return result;
    }

    public boolean triggerGuideGroup(int groupId) {
        return groupId > 0 && !getCompletedGuideGroupIds().contains(groupId)
                && getTriggeredGuideGroupIds().add(groupId);
    }

    public boolean completeGuideGroup(int groupId) {
        if (groupId <= 0) return false;
        getTriggeredGuideGroupIds().add(groupId);
        return getCompletedGuideGroupIds().add(groupId);
    }

    public boolean hasTriggeredGuideGroup(int groupId) {
        return getTriggeredGuideGroupIds().contains(groupId);
    }

    public boolean hasCompletedGuideGroup(int groupId) {
        return getCompletedGuideGroupIds().contains(groupId);
    }

    public List<Integer> pendingGuideGroupIds() {
        List<Integer> pending = new ArrayList<>();
        for (Integer groupId : getTriggeredGuideGroupIds()) {
            if (!getCompletedGuideGroupIds().contains(groupId)) pending.add(groupId);
        }
        pending.sort(Integer::compareTo);
        return pending;
    }

    public int getCurrentCasinoId() {
        return currentCasinoId;
    }

    public void setCurrentCasinoId(int currentCasinoId) {
        this.currentCasinoId = currentCasinoId;
    }

    public long getLastOfflineTime() {
        return lastOfflineTime;
    }

    public void setLastOfflineTime(long lastOfflineTime) {
        this.lastOfflineTime = lastOfflineTime;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public Map<Integer, Integer> getDailyDropCount() {
        return dailyDropCount;
    }

    public void setDailyDropCount(Map<Integer, Integer> dailyDropCount) {
        this.dailyDropCount = dailyDropCount;
    }

    public int getDropResetDay() {
        return dropResetDay;
    }

    public void setDropResetDay(int dropResetDay) {
        this.dropResetDay = dropResetDay;
    }

    public int getAllLevel() {
        return allLevel;
    }

    public void setAllLevel(int allLevel) {
        this.allLevel = allLevel;
    }

    public long getReceptionCount() {
        return receptionCount;
    }

    public void setReceptionCount(long receptionCount) {
        this.receptionCount = receptionCount;
    }

    public void addReceptionCount(long count) {
        if (count > 0) {
            this.receptionCount += count;
        }
    }

    public long getBusinessIncome() {
        return businessIncome;
    }

    public void setBusinessIncome(long businessIncome) {
        this.businessIncome = businessIncome;
    }

    public void addBusinessIncome(long gold) {
        if (gold > 0) {
            this.businessIncome += gold;
        }
    }

    public int getWatchAdCount() {
        return watchAdCount;
    }

    public void setWatchAdCount(int watchAdCount) {
        this.watchAdCount = watchAdCount;
    }

    public void incWatchAdCount() {
        this.watchAdCount++;
    }

    public int getFinishedTaskCount() {
        return finishedTaskCount;
    }

    public void setFinishedTaskCount(int finishedTaskCount) {
        this.finishedTaskCount = finishedTaskCount;
    }

    public void incFinishedTaskCount() {
        this.finishedTaskCount++;
    }

    public int getLastLoginDay() {
        return lastLoginDay;
    }

    public void setLastLoginDay(int lastLoginDay) {
        this.lastLoginDay = lastLoginDay;
    }

    public Map<Integer, SlotGameStatsData> getSlotStatsMap() {
        return slotStatsMap;
    }

    public void setSlotStatsMap(Map<Integer, SlotGameStatsData> slotStatsMap) {
        this.slotStatsMap = slotStatsMap;
    }

    public SlotGameStatsData findSlotStats(int gameType) {
        return slotStatsMap == null ? null : slotStatsMap.get(gameType);
    }

    public SlotGameStatsData findOrCreateSlotStats(int gameType) {
        if (slotStatsMap == null) {
            slotStatsMap = new HashMap<>();
        }
        return slotStatsMap.computeIfAbsent(gameType, ignored -> new SlotGameStatsData());
    }

    public boolean isOperationStatsMigrated() {
        return operationStatsMigrated;
    }

    public void setOperationStatsMigrated(boolean operationStatsMigrated) {
        this.operationStatsMigrated = operationStatsMigrated;
    }

    /**
     * 跨天则重置每日掉落计数
     *
     * @param today yyyyMMdd
     */
    public void checkResetDropCount(int today) {
        if (this.dropResetDay != today) {
            this.dropResetDay = today;
            if (this.dailyDropCount != null) {
                this.dailyDropCount.clear();
            }
        }
    }

    /**
     * 查询某 dropItem 当日已掉落次数
     */
    public int getDropCount(int dropItemId) {
        if (this.dailyDropCount == null || this.dailyDropCount.isEmpty()) {
            return 0;
        }
        return this.dailyDropCount.getOrDefault(dropItemId, 0);
    }

    /**
     * 某 dropItem 当日掉落次数 +1
     */
    public void addDropCount(int dropItemId) {
        if (this.dailyDropCount == null) {
            this.dailyDropCount = new HashMap<>();
        }
        this.dailyDropCount.merge(dropItemId, 1, Integer::sum);
    }

    public void addAllLevel(int level) {
        this.allLevel += level;
    }

    public Set<Integer> getAllMedalIds() {
        return allMedalIds;
    }

    public void setAllMedalIds(Set<Integer> allMedalIds) {
        this.allMedalIds = allMedalIds == null ? null : new HashSet<>(allMedalIds);
    }

    public boolean isMedalDataMigrated() {
        return medalDataMigrated;
    }

    public void setMedalDataMigrated(boolean medalDataMigrated) {
        this.medalDataMigrated = medalDataMigrated;
    }

    public List<Integer> getShowMedalIds() {
        return showMedalIds;
    }

    public void setShowMedalIds(List<Integer> showMedalIds) {
        this.showMedalIds = showMedalIds;
    }

    public void activeMedalId(int id) {
        if (this.allMedalIds == null) {
            this.allMedalIds = new HashSet<>();
        }
        this.allMedalIds.add(id);
    }
}
