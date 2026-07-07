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
    //上次离线时间 (ms), 用于长/短时掉线判定
    private long lastOfflineTime;
    //能量值
    private int power;
    //每日掉落次数 (dropItemId -> 当日已掉次数)
    private Map<Integer, Integer> dailyDropCount;
    //每日掉落计数重置日 (yyyyMMdd)
    private int dropResetDay;
    //研究点 (类型 -> 数量; 类型: 1.普通 2.珍惜)
    private Map<Integer, Integer> researchPointMap;
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

    public Map<Integer, Integer> getResearchPointMap() {
        return researchPointMap;
    }

    public void setResearchPointMap(Map<Integer, Integer> researchPointMap) {
        this.researchPointMap = researchPointMap;
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

    public void addResearchPoint(int type, int num) {
        if (this.researchPointMap == null) {
            this.researchPointMap = new HashMap<>();
        }
        this.researchPointMap.merge(type, num, Integer::sum);
    }

    /**
     * 查询某类型研究点的当前数量
     */
    public int findResearchPoint(int type) {
        if (this.researchPointMap == null || this.researchPointMap.isEmpty()) {
            return 0;
        }
        Integer v = this.researchPointMap.get(type);
        return v == null ? 0 : v;
    }

    /**
     * 扣除研究点 (内部已做余额校验); 余额不足返回 false
     */
    public boolean deductResearchPoint(int type, int points) {
        if (this.researchPointMap == null || this.researchPointMap.isEmpty()) {
            return false;
        }
        Integer before = this.researchPointMap.get(type);
        if (before == null || before < points) {
            return false;
        }
        int after = before - points;
        if (after < 1) {
            this.researchPointMap.remove(type);
        } else {
            this.researchPointMap.put(type, after);
        }
        return true;
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
