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
     * 托管中的玩家，持续到玩家主动取消托管
     */
    private final Set<Long> hostingPlayerIds = new HashSet<>();

    /**
     * 得证大道/隐忍渡劫待生效的玩家：playerId -> 规则类型(1得证大道 2隐忍渡劫)，
     * 在下一回合补牌阶段(DouXianDealPhase)生效并消费掉，DESIGN.md 三/8.13
     */
    private final Map<Long, Integer> pendingSpecialRule = new HashMap<>();

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

    public Map<Long, Integer> getPendingSpecialRule() {
        return pendingSpecialRule;
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
        pendingSpecialRule.clear();
        rechargingPlayerIds.clear();
        gameStartBalance.clear();
        roundStartBalance.clear();
    }
}
