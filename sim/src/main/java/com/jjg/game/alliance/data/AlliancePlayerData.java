package com.jjg.game.alliance.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家侧联盟数据 (每玩家一文档, 跟随玩家、不随换盟/解散重置)。
 * <p>
 * 需求明确: 贡献值、商店限购记录、活动参与次数都属于玩家, 换盟后继续生效——
 * 因此与联盟文档分离建模。同一玩家的请求经 Disruptor 按 playerId 串行,
 * 本文档的"读改写"(每日计数等)无并发竞态; 贡献值扣减仍走条件 $inc 兜底(防 GM/跨入口并发)。
 *
 * @author 11
 * @date 2026/6/11
 */
@Document("alliancePlayerData")
public class AlliancePlayerData {
    @Id
    private long playerId;

    //当前所在联盟 (0=无; 加入用"0 -> aid"条件占位防并发加两个盟)
    private long allianceId;
    //已创建过的联盟 id (每用户最多创建 1 个; 解散后不清零==终身一次, 需求"最多创建1个")
    private long createdAllianceId;
    //入盟时间(ms)
    private long joinTime;
    //有申请记录的联盟 id (反向索引: 入盟成功/退盟时据此反查清理各联盟侧的申请;
    //允许残留被拒/过期/被挤掉的陈旧项, 清理时按空操作处理)
    private List<Long> appliedAllianceIds = new ArrayList<>();

    //联盟贡献值 (流通货币, 退盟保留; 无盟时不可使用)
    private long contribution;
    //累计贡献度 (历史给联盟提供的声誉值总量, 展示用; 周榜单独走 Redis zset)
    private long contributionTotal;

    //--------- 每日计数 (day 字段 = yyyyMMdd, 跨天惰性重置) ---------
    //捐献
    private int donateDay;
    private int donateCount;
    //任务完成次数
    private int taskDay;
    private int taskFinishCount;
    //任务刷新次数
    private int refreshDay;
    private int refreshCount;
    //任务求助次数
    private int seekHelpDay;
    private int seekHelpCount;
    //帮助次数 (仅建筑加速类, global 表 226 上限)
    private int helpDay;
    private int helpCount;
    //建筑加速被帮助次数 (求助者视角, global 表 225 上限)
    private int speedupHelpedDay;
    private int speedupHelpedCount;
    //建筑加速求助(分享)次数 (global 表 249 上限)
    private int speedupSeekDay;
    private int speedupSeekCount;
    //商店限购 goodsId -> 当日已购数量
    private int shopDay;
    private Map<Integer, Integer> shopPurchases = new HashMap<>();

    //当前接取的任务 (null=未接取)
    private PlayerTakenTask takenTask;
    //已完成任务 (最近 N 条滚动保留, 最早的被挤掉; 供"已完成任务"查询)
    private List<PlayerTakenTask> finishedTasks = new ArrayList<>();
    //放弃任务冷却截止时间(ms)
    private long abandonCdUntil;

    //对决阶段奖励领取位图 period -> bitmask (第 n 位 = 第 n 阶段已领取; 仅保留近几期, 写入时清理)
    private Map<String, Integer> battleClaims = new HashMap<>();

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(long allianceId) {
        this.allianceId = allianceId;
    }

    public long getCreatedAllianceId() {
        return createdAllianceId;
    }

    public void setCreatedAllianceId(long createdAllianceId) {
        this.createdAllianceId = createdAllianceId;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }

    public List<Long> getAppliedAllianceIds() {
        return appliedAllianceIds;
    }

    public void setAppliedAllianceIds(List<Long> appliedAllianceIds) {
        this.appliedAllianceIds = appliedAllianceIds == null ? new ArrayList<>() : appliedAllianceIds;
    }

    public long getContribution() {
        return contribution;
    }

    public void setContribution(long contribution) {
        this.contribution = contribution;
    }

    public long getContributionTotal() {
        return contributionTotal;
    }

    public void setContributionTotal(long contributionTotal) {
        this.contributionTotal = contributionTotal;
    }

    public int getDonateDay() {
        return donateDay;
    }

    public void setDonateDay(int donateDay) {
        this.donateDay = donateDay;
    }

    public int getDonateCount() {
        return donateCount;
    }

    public void setDonateCount(int donateCount) {
        this.donateCount = donateCount;
    }

    public int getTaskDay() {
        return taskDay;
    }

    public void setTaskDay(int taskDay) {
        this.taskDay = taskDay;
    }

    public int getTaskFinishCount() {
        return taskFinishCount;
    }

    public void setTaskFinishCount(int taskFinishCount) {
        this.taskFinishCount = taskFinishCount;
    }

    public int getRefreshDay() {
        return refreshDay;
    }

    public void setRefreshDay(int refreshDay) {
        this.refreshDay = refreshDay;
    }

    public int getRefreshCount() {
        return refreshCount;
    }

    public void setRefreshCount(int refreshCount) {
        this.refreshCount = refreshCount;
    }

    public int getSeekHelpDay() {
        return seekHelpDay;
    }

    public void setSeekHelpDay(int seekHelpDay) {
        this.seekHelpDay = seekHelpDay;
    }

    public int getSeekHelpCount() {
        return seekHelpCount;
    }

    public void setSeekHelpCount(int seekHelpCount) {
        this.seekHelpCount = seekHelpCount;
    }

    public int getHelpDay() {
        return helpDay;
    }

    public void setHelpDay(int helpDay) {
        this.helpDay = helpDay;
    }

    public int getHelpCount() {
        return helpCount;
    }

    public void setHelpCount(int helpCount) {
        this.helpCount = helpCount;
    }

    public int getSpeedupHelpedDay() {
        return speedupHelpedDay;
    }

    public void setSpeedupHelpedDay(int speedupHelpedDay) {
        this.speedupHelpedDay = speedupHelpedDay;
    }

    public int getSpeedupHelpedCount() {
        return speedupHelpedCount;
    }

    public void setSpeedupHelpedCount(int speedupHelpedCount) {
        this.speedupHelpedCount = speedupHelpedCount;
    }

    public int getSpeedupSeekDay() {
        return speedupSeekDay;
    }

    public void setSpeedupSeekDay(int speedupSeekDay) {
        this.speedupSeekDay = speedupSeekDay;
    }

    public int getSpeedupSeekCount() {
        return speedupSeekCount;
    }

    public void setSpeedupSeekCount(int speedupSeekCount) {
        this.speedupSeekCount = speedupSeekCount;
    }

    public int getShopDay() {
        return shopDay;
    }

    public void setShopDay(int shopDay) {
        this.shopDay = shopDay;
    }

    public Map<Integer, Integer> getShopPurchases() {
        return shopPurchases;
    }

    public void setShopPurchases(Map<Integer, Integer> shopPurchases) {
        this.shopPurchases = shopPurchases == null ? new HashMap<>() : shopPurchases;
    }

    public PlayerTakenTask getTakenTask() {
        return takenTask;
    }

    public void setTakenTask(PlayerTakenTask takenTask) {
        this.takenTask = takenTask;
    }

    public List<PlayerTakenTask> getFinishedTasks() {
        return finishedTasks;
    }

    public void setFinishedTasks(List<PlayerTakenTask> finishedTasks) {
        this.finishedTasks = finishedTasks == null ? new ArrayList<>() : finishedTasks;
    }

    public long getAbandonCdUntil() {
        return abandonCdUntil;
    }

    public void setAbandonCdUntil(long abandonCdUntil) {
        this.abandonCdUntil = abandonCdUntil;
    }

    public Map<String, Integer> getBattleClaims() {
        return battleClaims;
    }

    public void setBattleClaims(Map<String, Integer> battleClaims) {
        this.battleClaims = battleClaims == null ? new HashMap<>() : battleClaims;
    }

    // ---------------------------------------------------------------------
    // 每日计数便捷取值 (跨天自动视为 0)
    // ---------------------------------------------------------------------

    public boolean inAlliance() {
        return allianceId > 0;
    }

    public int donateCountOf(int today) {
        return donateDay == today ? donateCount : 0;
    }

    public int taskFinishCountOf(int today) {
        return taskDay == today ? taskFinishCount : 0;
    }

    public int seekHelpCountOf(int today) {
        return seekHelpDay == today ? seekHelpCount : 0;
    }

    public int helpCountOf(int today) {
        return helpDay == today ? helpCount : 0;
    }

    public int speedupSeekCountOf(int today) {
        return speedupSeekDay == today ? speedupSeekCount : 0;
    }

    public int shopPurchasedOf(int today, int goodsId) {
        if (shopDay != today || shopPurchases == null) {
            return 0;
        }
        Integer n = shopPurchases.get(goodsId);
        return n == null ? 0 : n;
    }

    public int battleClaimedMask(String period) {
        if (battleClaims == null) {
            return 0;
        }
        Integer mask = battleClaims.get(period);
        return mask == null ? 0 : mask;
    }

    public int refreshCountOf(int today) {
        return refreshDay == today ? refreshCount : 0;
    }
}
