package com.jjg.game.season.data;

import com.alibaba.fastjson.annotation.JSONField;
import com.jjg.game.sim.data.AbstractData;
import com.jjg.game.season.model.SeasonPhase;
import com.jjg.game.season.model.SeasonSnapshot;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家赛季聚合数据。赛季内高频状态集中在一个文档中，避免一次操作写多个集合。
 */
@Document
public class SeasonPlayerData extends AbstractData {
    @Id
    private long playerId;
    //昵称/头像冗余在赛季文档上, 匹配/榜单等跨玩家展示直接随文档读出, 避免按 ID 查玩家表
    private String playerName;
    private int headImgId;
    private int headFrameId;
    private int seasonId;
    private long timelineOrigin;
    private long gmTimeOffset;
    private String seasonKey;
    private String phase;
    private int cycleIndex;
    private long startTime;
    private long endTime;
    private long seasonCoin;
    private long totalEarnedCoin;
    private int tierId;
    private int dailyKey;
    private int dailyMatchCount;
    private long dailyWinAmount;
    private long dailyLossAmount;
    private long dailyPostLimitBet;
    private int dailyGemDropCount;
    private int dailyFreeGameUsed;
    private long lastMatchTime;
    private Map<Integer, Integer> equippedGems = new HashMap<>();
    private Map<Integer, Integer> shopPurchases = new HashMap<>();
    private Map<Integer, Integer> dailyShopPurchases = new HashMap<>();
    private Map<Integer, Integer> trialStars = new HashMap<>();
    private SeasonTrialSession activeTrial;
    private List<Long> representativeSpinWins = new ArrayList<>();
    private long representativeStake;
    private int representativeGameType;
    private SeasonMatchSession activeMatch;
    //循环赛季对局中掉线的时刻 (赛季时间, 毫秒); 0 表示无掉线标记
    private long matchOfflineTime;
    //上赛季结算快照; 跨季后首次请求赛季信息时下发并清除 (切季时生成, startSeason 不得重置)
    private SeasonSettlement lastSettlement;
    private List<SeasonMatchRecord> matchHistory = new ArrayList<>();
    private List<String> processedMatchIds = new ArrayList<>();
    //上次拉取跨节点待结算记录的时间; 内存态不落库 (登录后首次访问必拉)
    @Transient
    @JSONField(serialize = false, deserialize = false)
    private transient long lastPendingCheckTime;
    //展示用名次缓存 (玩家线程本地, 不落库); 币值变化或 TTL 过期时由 SeasonRankingService 重查
    @Transient
    @JSONField(serialize = false, deserialize = false)
    private transient long rankCacheTime;
    @Transient
    @JSONField(serialize = false, deserialize = false)
    private transient long rankCacheCoin;
    @Transient
    @JSONField(serialize = false, deserialize = false)
    private transient long rankCacheEarned;
    @Transient
    @JSONField(serialize = false, deserialize = false)
    private transient int rankCacheValue;

    public void startSeason(SeasonSnapshot snapshot, long initialCoin) {
        seasonId = snapshot.seasonId();
        seasonKey = snapshot.seasonKey();
        phase = snapshot.phase().name();
        cycleIndex = snapshot.cycleIndex();
        startTime = snapshot.startTime();
        endTime = snapshot.endTime();
        seasonCoin = Math.max(0, initialCoin);
        totalEarnedCoin = 0;
        tierId = 0;
        dailyKey = 0;
        resetDailyCounters();
        lastMatchTime = 0;
        getEquippedGems().clear();
        getShopPurchases().clear();
        getDailyShopPurchases().clear();
        getTrialStars().clear();
        activeTrial = null;
        getRepresentativeSpinWins().clear();
        representativeStake = 0;
        representativeGameType = 0;
        activeMatch = null;
        matchOfflineTime = 0;
        getMatchHistory().clear();
        getProcessedMatchIds().clear();
        rankCacheTime = 0;
    }

    public boolean resetDaily(int newDailyKey) {
        if (dailyKey == newDailyKey) {
            return false;
        }
        dailyKey = newDailyKey;
        resetDailyCounters();
        getDailyShopPurchases().clear();
        return true;
    }

    private void resetDailyCounters() {
        dailyMatchCount = 0;
        dailyWinAmount = 0;
        dailyLossAmount = 0;
        dailyPostLimitBet = 0;
        dailyGemDropCount = 0;
        dailyFreeGameUsed = 0;
    }

    public void addMatchRecord(SeasonMatchRecord record, int limit) {
        if (record == null || limit <= 0) {
            return;
        }
        List<SeasonMatchRecord> records = getMatchHistory();
        records.add(0, record);
        if (records.size() > limit) {
            records.subList(limit, records.size()).clear();
        }
    }

    public boolean markMatchProcessed(String matchId, int limit) {
        if (matchId == null || matchId.isBlank() || limit <= 0) {
            return false;
        }
        List<String> ids = getProcessedMatchIds();
        if (ids.contains(matchId)) {
            return false;
        }
        ids.add(0, matchId);
        if (ids.size() > limit) {
            ids.subList(limit, ids.size()).clear();
        }
        return true;
    }

    /**
     * 循环赛季对局中掉线时记录离线时刻 (赛季时间); 再次进入赛季时据此判定是否自动补完剩余局。
     */
    public void markMatchOffline(long systemTime) {
        if (activeMatch != null && seasonPhase() == SeasonPhase.LOOP) {
            matchOfflineTime = Math.addExact(systemTime, gmTimeOffset);
        }
    }

    public SeasonPhase seasonPhase() {
        return phase == null ? null : SeasonPhase.valueOf(phase);
    }

    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getHeadImgId() { return headImgId; }
    public void setHeadImgId(int headImgId) { this.headImgId = headImgId; }
    public int getHeadFrameId() { return headFrameId; }
    public void setHeadFrameId(int headFrameId) { this.headFrameId = headFrameId; }
    public int getSeasonId() { return seasonId; }
    public void setSeasonId(int seasonId) { this.seasonId = seasonId; }
    public long getTimelineOrigin() { return timelineOrigin; }
    public void setTimelineOrigin(long timelineOrigin) { this.timelineOrigin = timelineOrigin; }
    public long getGmTimeOffset() { return gmTimeOffset; }
    public void setGmTimeOffset(long gmTimeOffset) { this.gmTimeOffset = gmTimeOffset; }
    public String getSeasonKey() { return seasonKey; }
    public void setSeasonKey(String seasonKey) { this.seasonKey = seasonKey; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public int getCycleIndex() { return cycleIndex; }
    public void setCycleIndex(int cycleIndex) { this.cycleIndex = cycleIndex; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public long getSeasonCoin() { return seasonCoin; }
    public void setSeasonCoin(long seasonCoin) { this.seasonCoin = Math.max(0, seasonCoin); }
    public long getTotalEarnedCoin() { return totalEarnedCoin; }
    public void setTotalEarnedCoin(long totalEarnedCoin) { this.totalEarnedCoin = Math.max(0, totalEarnedCoin); }
    public int getTierId() { return tierId; }
    public void setTierId(int tierId) { this.tierId = tierId; }
    public int getDailyKey() { return dailyKey; }
    public void setDailyKey(int dailyKey) { this.dailyKey = dailyKey; }
    public int getDailyMatchCount() { return dailyMatchCount; }
    public void setDailyMatchCount(int dailyMatchCount) { this.dailyMatchCount = dailyMatchCount; }
    public long getDailyWinAmount() { return dailyWinAmount; }
    public void setDailyWinAmount(long dailyWinAmount) { this.dailyWinAmount = dailyWinAmount; }
    public long getDailyLossAmount() { return dailyLossAmount; }
    public void setDailyLossAmount(long dailyLossAmount) { this.dailyLossAmount = dailyLossAmount; }
    public long getDailyPostLimitBet() { return dailyPostLimitBet; }
    public void setDailyPostLimitBet(long dailyPostLimitBet) { this.dailyPostLimitBet = dailyPostLimitBet; }
    public int getDailyGemDropCount() { return dailyGemDropCount; }
    public void setDailyGemDropCount(int dailyGemDropCount) { this.dailyGemDropCount = dailyGemDropCount; }
    public int getDailyFreeGameUsed() { return dailyFreeGameUsed; }
    public void setDailyFreeGameUsed(int dailyFreeGameUsed) { this.dailyFreeGameUsed = dailyFreeGameUsed; }
    public long getLastMatchTime() { return lastMatchTime; }
    public void setLastMatchTime(long lastMatchTime) { this.lastMatchTime = lastMatchTime; }

    public Map<Integer, Integer> getEquippedGems() {
        if (equippedGems == null) equippedGems = new HashMap<>();
        return equippedGems;
    }
    public void setEquippedGems(Map<Integer, Integer> equippedGems) { this.equippedGems = equippedGems == null ? new HashMap<>() : equippedGems; }
    public Map<Integer, Integer> getShopPurchases() {
        if (shopPurchases == null) shopPurchases = new HashMap<>();
        return shopPurchases;
    }
    public void setShopPurchases(Map<Integer, Integer> shopPurchases) { this.shopPurchases = shopPurchases == null ? new HashMap<>() : shopPurchases; }
    public Map<Integer, Integer> getDailyShopPurchases() {
        if (dailyShopPurchases == null) dailyShopPurchases = new HashMap<>();
        return dailyShopPurchases;
    }
    public void setDailyShopPurchases(Map<Integer, Integer> dailyShopPurchases) { this.dailyShopPurchases = dailyShopPurchases == null ? new HashMap<>() : dailyShopPurchases; }
    public Map<Integer, Integer> getTrialStars() {
        if (trialStars == null) trialStars = new HashMap<>();
        return trialStars;
    }
    public void setTrialStars(Map<Integer, Integer> trialStars) { this.trialStars = trialStars == null ? new HashMap<>() : trialStars; }
    public SeasonTrialSession getActiveTrial() { return activeTrial; }
    public void setActiveTrial(SeasonTrialSession activeTrial) { this.activeTrial = activeTrial; }
    public List<Long> getRepresentativeSpinWins() {
        if (representativeSpinWins == null) representativeSpinWins = new ArrayList<>();
        return representativeSpinWins;
    }
    public void setRepresentativeSpinWins(List<Long> representativeSpinWins) { this.representativeSpinWins = representativeSpinWins == null ? new ArrayList<>() : new ArrayList<>(representativeSpinWins); }
    public long getRepresentativeStake() { return representativeStake; }
    public void setRepresentativeStake(long representativeStake) { this.representativeStake = representativeStake; }
    public int getRepresentativeGameType() { return representativeGameType; }
    public void setRepresentativeGameType(int representativeGameType) { this.representativeGameType = representativeGameType; }
    public SeasonMatchSession getActiveMatch() { return activeMatch; }
    public void setActiveMatch(SeasonMatchSession activeMatch) { this.activeMatch = activeMatch; }
    public long getMatchOfflineTime() { return matchOfflineTime; }
    public void setMatchOfflineTime(long matchOfflineTime) { this.matchOfflineTime = matchOfflineTime; }
    public SeasonSettlement getLastSettlement() { return lastSettlement; }
    public void setLastSettlement(SeasonSettlement lastSettlement) { this.lastSettlement = lastSettlement; }
    public List<SeasonMatchRecord> getMatchHistory() {
        if (matchHistory == null) matchHistory = new ArrayList<>();
        return matchHistory;
    }
    public void setMatchHistory(List<SeasonMatchRecord> matchHistory) { this.matchHistory = matchHistory == null ? new ArrayList<>() : matchHistory; }
    public List<String> getProcessedMatchIds() {
        if (processedMatchIds == null) processedMatchIds = new ArrayList<>();
        return processedMatchIds;
    }
    public void setProcessedMatchIds(List<String> processedMatchIds) { this.processedMatchIds = processedMatchIds == null ? new ArrayList<>() : processedMatchIds; }
    @JSONField(serialize = false, deserialize = false)
    public long getLastPendingCheckTime() { return lastPendingCheckTime; }
    public void setLastPendingCheckTime(long lastPendingCheckTime) { this.lastPendingCheckTime = lastPendingCheckTime; }
    @JSONField(serialize = false, deserialize = false)
    public long getRankCacheTime() { return rankCacheTime; }
    public void setRankCacheTime(long rankCacheTime) { this.rankCacheTime = rankCacheTime; }
    @JSONField(serialize = false, deserialize = false)
    public long getRankCacheCoin() { return rankCacheCoin; }
    public void setRankCacheCoin(long rankCacheCoin) { this.rankCacheCoin = rankCacheCoin; }
    @JSONField(serialize = false, deserialize = false)
    public long getRankCacheEarned() { return rankCacheEarned; }
    public void setRankCacheEarned(long rankCacheEarned) { this.rankCacheEarned = rankCacheEarned; }
    @JSONField(serialize = false, deserialize = false)
    public int getRankCacheValue() { return rankCacheValue; }
    public void setRankCacheValue(int rankCacheValue) { this.rankCacheValue = rankCacheValue; }
}
