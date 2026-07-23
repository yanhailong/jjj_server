package com.jjg.game.season.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * slots 会话内的赛季运行态。
 *
 * <p>sim 节点负责生成赛季币和 {@code SeasonGem} 效果快照，slots 节点补充入口类型及每日
 * 免费局等本地会话状态。一个玩家会话只持有一个对象，避免赛季状态散落在
 * {@code SlotsPlayerGameData} 中。宝石效果在会话内只读，赛季币余额随结算更新，从而避免
 * 每次旋转跨节点查询或重复聚合配置。集合统一复制成 {@link ArrayList}/{@link HashMap}，
 * 避免 Protostuff 反序列化不可变集合时失败；普通入口不会使用的集合采用延迟创建，减少
 * 会话常驻对象。该对象归属 slots 玩家工作线程，不使用并发集合，也不允许跨线程共享可变实例。</p>
 */
public class SeasonSlotsSessionData {
    /** 是否由赛季入口进入；仅此标记为 true 时使用赛季币及 SeasonGem 效果。 */
    private boolean seasonEnter;
    /** 赛季入口创建会话时的赛季币余额，后续由 slots 结算结果持续更新。 */
    private long seasonCoin;
    /** 当前机台是否是赛季每日免费局候选机台。 */
    private boolean seasonFreeGameCandidate;
    /** 当日免费次数已耗尽的系统日期 key（yyyyMMdd），跨天后允许重新向 sim 申请。 */
    private int seasonFreeExhaustedDailyKey;
    /** 由所有匹配当前游戏的已镶嵌宝石解锁的下注额，已去重。 */
    private List<Long> bet;
    /** 聚合 SeasonGem.specialMode 后的结果库类型权重增量。 */
    private Map<Integer, Integer> libTypeWeightDelta;
    /** 聚合 SeasonGem.winRate 与 specialModeProbUp 后的结果区间权重增量。 */
    private Map<Integer, Map<Integer, Integer>> sectionWeightDelta;

    public boolean isSeasonEnter() {
        return seasonEnter;
    }

    public void setSeasonEnter(boolean seasonEnter) {
        this.seasonEnter = seasonEnter;
    }

    public long getSeasonCoin() {
        return seasonCoin;
    }

    public void setSeasonCoin(long seasonCoin) {
        this.seasonCoin = seasonCoin;
    }

    public boolean isSeasonFreeGameCandidate() {
        return seasonFreeGameCandidate;
    }

    public void setSeasonFreeGameCandidate(boolean seasonFreeGameCandidate) {
        this.seasonFreeGameCandidate = seasonFreeGameCandidate;
    }

    public int getSeasonFreeExhaustedDailyKey() {
        return seasonFreeExhaustedDailyKey;
    }

    public void setSeasonFreeExhaustedDailyKey(int seasonFreeExhaustedDailyKey) {
        this.seasonFreeExhaustedDailyKey = seasonFreeExhaustedDailyKey;
    }

    public List<Long> getBet() {
        if (bet == null) {
            bet = new ArrayList<>();
        }
        return bet;
    }

    public void setBet(List<Long> bet) {
        this.bet = bet == null ? new ArrayList<>() : new ArrayList<>(bet);
    }

    public Map<Integer, Integer> getLibTypeWeightDelta() {
        if (libTypeWeightDelta == null) {
            libTypeWeightDelta = new HashMap<>();
        }
        return libTypeWeightDelta;
    }

    public void setLibTypeWeightDelta(Map<Integer, Integer> libTypeWeightDelta) {
        this.libTypeWeightDelta = libTypeWeightDelta == null
                ? new HashMap<>() : new HashMap<>(libTypeWeightDelta);
    }

    public Map<Integer, Map<Integer, Integer>> getSectionWeightDelta() {
        if (sectionWeightDelta == null) {
            sectionWeightDelta = new HashMap<>();
        }
        return sectionWeightDelta;
    }

    public void setSectionWeightDelta(Map<Integer, Map<Integer, Integer>> sectionWeightDelta) {
        this.sectionWeightDelta = new HashMap<>();
        if (sectionWeightDelta == null) {
            return;
        }
        sectionWeightDelta.forEach((libType, delta) -> this.sectionWeightDelta.put(
                libType, delta == null ? new HashMap<>() : new HashMap<>(delta)));
    }
}
