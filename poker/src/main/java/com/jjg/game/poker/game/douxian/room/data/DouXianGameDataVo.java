package com.jjg.game.poker.game.douxian.room.data;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.data.DouXianDataHelper;
import com.jjg.game.poker.game.douxian.data.DouXianZoneCards;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 斗仙牌整局数据。
 */
public class DouXianGameDataVo extends BasePokerGameDataVo {

    /**
     * 玩家id -> 手牌(pokerPool配置id)
     */
    private final Map<Long, List<Integer>> handCards = new HashMap<>();

    /**
     * 玩家id -> 区域 -> 该区域已摆的牌
     */
    private final Map<Long, Map<DouXianZone, DouXianZoneCards>> playerZoneCards = new HashMap<>();

    /**
     * 本回合已确认出牌的玩家，每回合进入出牌阶段时清空
     */
    private final Set<Long> confirmedPlayerIds = new HashSet<>();

    /**
     * 本回合已经处理过弃牌请求(含"不弃")的玩家，每回合进入弃牌阶段时清空
     */
    private final Set<Long> discardedPlayerIds = new HashSet<>();

    /**
     * 已经认输的玩家，认输后持续到本局结束
     */
    private final Set<Long> concededPlayerIds = new HashSet<>();

    /**
     * 托管中的玩家，持续到玩家主动取消托管(不会因为进入新回合而自动清除)
     */
    private final Set<Long> hostingPlayerIds = new HashSet<>();

    /**
     * 当前出牌阶段主动取消过托管的玩家。用于避免取消请求与本阶段超时回调相邻执行时，
     * 玩家刚取消托管又被同一个阶段重新加入托管；进入下一次出牌阶段时清空。
     */
    private final Set<Long> hostingCancelledPlayerIdsThisPhase = new HashSet<>();

    /**
     * 得证大道/隐忍渡劫待生效的玩家：playerId -> 规则类型(1得证大道 2隐忍渡劫)，
     * 在下一回合补牌阶段(DouXianDealPhase)生效并消费掉，DESIGN.md 三/8.13
     */
    private final Map<Long, Integer> pendingSpecialRule = new HashMap<>();

    /**
     * GM强制本回合触发的特殊规则：playerId -> 规则类型(1得证大道 2隐忍渡劫)。
     * 结算阶段读取一次后立即清空，不影响后续回合。
     */
    private final Map<Long, Integer> gmForcedSpecialRule = new HashMap<>();

    /**
     * 正在"即时充值复活"倒计时中的玩家，DESIGN.md 8.9
     */
    private final Set<Long> rechargingPlayerIds = new HashSet<>();

    /**
     * 玩家id -> 开局前携带金币，开局时(tryStartGame)拍一次快照，本局内不变。
     * DESIGN.md 6.2 "小额玩家保护"的判定依据之一。
     */
    private final Map<Long, Long> gameStartBalance = new HashMap<>();

    /**
     * 玩家id -> 本回合开始前携带金币，每回合开局/补牌阶段(DouXianDealPhase)重新拍一次快照。
     * DESIGN.md 6.2 "小额玩家保护"的判定依据之一。
     */
    private final Map<Long, Long> roundStartBalance = new HashMap<>();

    /**
     * 等待阶段(WAIT_READY)已确认准备的玩家，DESIGN.md 3.1 匹配阶段：仿南方前进，
     * 人齐(座位坐满)之后还需要全员都在这个集合里才会真正开局，每局结束resetData都会清空。
     */
    private final Set<Long> readyPlayerIds = new HashSet<>();

    /**
     * 已经安排过"自动准备"调度的机器人玩家id，避免 tryStartGame 被反复调用时重复下发调度，
     * 每局结束resetData都会清空，下一局会重新给还坐在位置上的机器人安排。
     */
    private final Set<Long> readyTimerScheduled = new HashSet<>();

    /**
     * 玩家id -> 每回合结算后的净输赢(不含充值复活换来的金币，那是兑换不是输赢)，下标0对应
     * 该玩家参与的第1个回合，只记录实际打过结算的回合(比如认输之后就不会再有新的记录)。
     * 大结算({@link com.jjg.game.poker.game.douxian.room.DouXianGameController#triggerGrandSettlement}
     * )直接用这个填 {@code DouXianGrandSettlementPlayerInfo.roundChangeList}，DESIGN.md 8.12
     */
    private final Map<Long, List<Long>> roundChangeList = new HashMap<>();

    public DouXianGameDataVo(Room_ChessCfg roomCfg) {
        super(roomCfg);
    }

    public Map<Long, List<Integer>> getHandCards() {
        return handCards;
    }

    public Map<DouXianZone, DouXianZoneCards> getPlayerZoneCards(long playerId) {
        return playerZoneCards.computeIfAbsent(playerId, key -> {
            Map<DouXianZone, DouXianZoneCards> map = new EnumMap<>(DouXianZone.class);
            for (DouXianZone zone : DouXianZone.values()) {
                map.put(zone, new DouXianZoneCards(zone));
            }
            return map;
        });
    }

    public Set<Long> getConfirmedPlayerIds() {
        return confirmedPlayerIds;
    }

    public Set<Long> getDiscardedPlayerIds() {
        return discardedPlayerIds;
    }

    public Set<Long> getConcededPlayerIds() {
        return concededPlayerIds;
    }

    public Set<Long> getHostingPlayerIds() {
        return hostingPlayerIds;
    }

    public Set<Long> getHostingCancelledPlayerIdsThisPhase() {
        return hostingCancelledPlayerIdsThisPhase;
    }

    public Map<Long, Integer> getPendingSpecialRule() {
        return pendingSpecialRule;
    }

    public Map<Long, Integer> getGmForcedSpecialRule() {
        return gmForcedSpecialRule;
    }

    public Set<Long> getRechargingPlayerIds() {
        return rechargingPlayerIds;
    }

    public Map<Long, Long> getGameStartBalance() {
        return gameStartBalance;
    }

    public Map<Long, Long> getRoundStartBalance() {
        return roundStartBalance;
    }

    public Set<Long> getReadyPlayerIds() {
        return readyPlayerIds;
    }

    public Set<Long> getReadyTimerScheduled() {
        return readyTimerScheduled;
    }

    public Map<Long, List<Long>> getRoundChangeList() {
        return roundChangeList;
    }

    /**
     * 结算阶段每回合调用一次，记一笔这个玩家本回合的净输赢
     */
    public void recordRoundChange(long playerId, long change) {
        roundChangeList.computeIfAbsent(playerId, key -> new ArrayList<>()).add(change);
    }

    /**
     * 当前还在游戏中的玩家id：座位没被标记删除、且没有认输
     */
    public List<Long> getActivePlayerIds() {
        List<Long> ids = new ArrayList<>();
        for (PlayerSeatInfo seatInfo : getPlayerSeatInfoList()) {
            if (seatInfo.isDelState()) {
                continue;
            }
            if (concededPlayerIds.contains(seatInfo.getPlayerId())) {
                continue;
            }
            ids.add(seatInfo.getPlayerId());
        }
        return ids;
    }

    @Override
    public int getPoolId() {
        return DouXianDataHelper.getPoolId(this);
    }

    @Override
    public void resetData(BasePokerGameController<? extends BasePokerGameDataVo> controller) {
        super.resetData(controller);
        handCards.clear();
        playerZoneCards.clear();
        confirmedPlayerIds.clear();
        discardedPlayerIds.clear();
        concededPlayerIds.clear();
        hostingPlayerIds.clear();
        hostingCancelledPlayerIdsThisPhase.clear();
        pendingSpecialRule.clear();
        gmForcedSpecialRule.clear();
        rechargingPlayerIds.clear();
        gameStartBalance.clear();
        roundStartBalance.clear();
        readyPlayerIds.clear();
        readyTimerScheduled.clear();
        roundChangeList.clear();
    }
}
