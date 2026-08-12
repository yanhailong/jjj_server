package com.jjg.game.poker.game.douxian.gamephase;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.gamephase.BasePokerPhase;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.data.DouXianBuilder;
import com.jjg.game.poker.game.douxian.data.DouXianDataHelper;
import com.jjg.game.poker.game.douxian.data.DouXianZoneCards;
import com.jjg.game.poker.game.douxian.message.bean.DouXianPairSettlementInfo;
import com.jjg.game.poker.game.douxian.message.bean.DouXianRevealInfo;
import com.jjg.game.poker.game.douxian.message.bean.DouXianSpecialRuleInfo;
import com.jjg.game.poker.game.douxian.message.bean.DouXianZoneSettlementInfo;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianSettlement;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianSpecialRuleTrigger;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.poker.game.douxian.util.DouXianHandEvaluator;
import com.jjg.game.poker.game.douxian.util.DouXianHandResult;
import com.jjg.game.poker.game.douxian.util.DouXianSettlementCalculator;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.sampledata.bean.ImmortalCardCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结算阶段，DESIGN.md 六：4人任意两两一组，按 凡界->灵界->仙界 顺序比灵力值。
 * 分两步走：先把整回合(含全胜二次结算)所有"理论应赔付"金额都算出来(不做任何封顶)，
 * 再对这一整批结果统一应用 DESIGN.md 6.2 的三层封顶(单笔封顶/小额玩家保护/输家余额不足按比例分摊)，
 * 最后才真正转账——不能算一笔转一笔，因为封顶规则本身是按"回合内赢家/输家维度"聚合的，
 * 见 {@link DouXianSettlementCalculator#applyRoundCaps}。
 * 三区域都开放时(第2回合起)才会判定全胜，第1回合仙界还没开放不触发全胜。
 */
public class DouXianSettlementPhase extends BasePokerPhase<DouXianGameDataVo> {

    private List<Long> needRechargePlayerIds = List.of();
    private int settlementEffectTime = DouXianConstant.Time.SETTLEMENT_ANIMATION_ACK_TIMEOUT;
    private boolean finished;

    public DouXianSettlementPhase(AbstractPhaseGameController<Room_ChessCfg, DouXianGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.GAME_ROUND_OVER_SETTLEMENT;
    }

    @Override
    public int getPhaseRunTime() {
        return settlementEffectTime;
    }

    @Override
    public void phaseDoAction() {
        if (gameController instanceof DouXianGameController douXianController) {
            douXianController.trackKafkaPhaseStart(getGamePhase());
        }
        if (!(gameController instanceof BasePokerGameController<DouXianGameDataVo> controller)) {
            super.phaseDoAction();
            return;
        }
        int round = gameDataVo.getRound();
        long betBase = gameDataVo.getRoomCfg().getBetBase();
        List<Long> activePlayerIds = gameDataVo.getActivePlayerIds();
        List<DouXianZone> openZones = new ArrayList<>();
        for (DouXianZone zone : DouXianZone.values()) {
            if (zone.isOpenAt(round)) {
                openZones.add(zone);
            }
        }

        log.info("---------- 斗仙牌结算开始 round:{} 参与玩家:{} 底分:{} ----------", round, activePlayerIds, betBase);

        Map<Long, Map<DouXianZone, DouXianHandResult>> playerZoneResults = new HashMap<>();
        for (Long playerId : activePlayerIds) {
            Map<DouXianZone, DouXianHandResult> zoneResults = new EnumMap<>(DouXianZone.class);
            StringBuilder sb = new StringBuilder();
            for (DouXianZone zone : openZones) {
                DouXianZoneCards zc = gameDataVo.getPlayerZoneCards(playerId).get(zone);
                List<com.jjg.game.core.data.Card> cards = DouXianDataHelper.toCards(gameDataVo, zc.getAllCards());
                DouXianHandResult result = DouXianHandEvaluator.evaluateZone(gameDataVo, zone, cards, round);
                zoneResults.put(zone, result);
                sb.append(zone).append(DouXianDataHelper.cardsToString(cards))
                        .append('=').append(result.getHandType().getDisplayName())
                        .append('(').append(result.getAetherValue()).append(") ");
            }
            playerZoneResults.put(playerId, zoneResults);
            log.info("斗仙牌结算-亮牌 playerId:{} {}", playerId, sb);
        }

        // 第一步：算出整回合所有理论结算金额(不封顶)，同时收集成一个平铺列表方便统一封顶
        List<DouXianPairSettlementInfo> pairResults = new ArrayList<>();
        List<DouXianZoneSettlementInfo> allDebts = new ArrayList<>();
        for (int i = 0; i < activePlayerIds.size(); i++) {
            for (int j = i + 1; j < activePlayerIds.size(); j++) {
                DouXianPairSettlementInfo pair = computePairTheoretical(activePlayerIds.get(i), activePlayerIds.get(j),
                        playerZoneResults, openZones, betBase);
                pairResults.add(pair);
                allDebts.addAll(pair.zoneResults);
                if (pair.grandWinExtraResults != null) {
                    allDebts.addAll(pair.grandWinExtraResults);
                }
                logPairTheoretical(pair);
            }
        }

        // 第二步：统一应用三层封顶(DESIGN.md 6.2)
        Map<Long, Long> currentBalance = new HashMap<>();
        for (Long playerId : activePlayerIds) {
            currentBalance.put(playerId, controller.getTransactionItemNum(playerId));
        }
        ImmortalCardCfg cardCfg = DouXianDataHelper.getImmortalCardCfg(gameDataVo);
        long maxCap = cardCfg != null ? cardCfg.getMaxCap() : Long.MAX_VALUE;
        long minWinLimit = cardCfg != null ? cardCfg.getWinLoss() : 0;
        if (cardCfg == null) {
            log.error("斗仙牌结算找不到ImmortalCardCfg，本次结算不做场次封顶/小额玩家保护 roomCfgId:{}",
                    gameDataVo.getRoomCfg().getId());
        }
        log.info("斗仙牌结算封顶参数 roomCfgId:{} maxCap:{} minWinLimit:{} 开局金币:{} 回合开始金币:{} 结算前实时金币:{}",
                gameDataVo.getRoomCfg().getId(), maxCap, minWinLimit,
                gameDataVo.getGameStartBalance(), gameDataVo.getRoundStartBalance(), currentBalance);

        List<Long> theoreticalAmounts = new ArrayList<>();
        for (DouXianZoneSettlementInfo debt : allDebts) {
            theoreticalAmounts.add(debt.changeValue);
        }
        DouXianSettlementCalculator.applyRoundCaps(allDebts, maxCap, minWinLimit,
                gameDataVo.getGameStartBalance(), gameDataVo.getRoundStartBalance(), currentBalance);

        // 第三步：封顶后的最终金额才是真实要转账的金额；同时按成功转账累计本回合净输赢，
        // 大结算直接使用这份结果，不再通过余额前后差值反推。
        Map<Long, Long> roundChanges = new HashMap<>();
        for (Long playerId : activePlayerIds) {
            roundChanges.put(playerId, 0L);
        }
        for (int i = 0; i < allDebts.size(); i++) {
            DouXianZoneSettlementInfo debt = allDebts.get(i);
            long theoretical = theoreticalAmounts.get(i);
            if (debt.winnerId == 0) {
                continue;
            }
            if (!applyTransfer(controller, debt.winnerId, debt.loserId, debt.changeValue)) {
                debt.changeValue = 0;
            } else {
                roundChanges.merge(debt.winnerId, debt.changeValue, Long::sum);
                roundChanges.merge(debt.loserId, -debt.changeValue, Long::sum);
            }
            if (theoretical != debt.changeValue) {
                log.info("斗仙牌结算封顶生效 zoneId:{} 赢家:{} 输家:{} 理论:{} -> 实际:{}(被裁减{})",
                        debt.zoneId, debt.winnerId, debt.loserId, theoretical, debt.changeValue, theoretical - debt.changeValue);
            } else {
                log.info("斗仙牌结算转账 zoneId:{} 赢家:{} 输家:{} 金额:{}",
                        debt.zoneId, debt.winnerId, debt.loserId, debt.changeValue);
            }
        }

        Map<Long, Long> balanceAfter = new HashMap<>();
        for (Long playerId : activePlayerIds) {
            balanceAfter.put(playerId, controller.getTransactionItemNum(playerId));
        }
        log.info("---------- 斗仙牌结算结束 round:{} 结算对数:{} 结算后金币:{} ----------",
                round, pairResults.size(), balanceAfter);

        // 大结算记录实际成功结算的净额；失败并完成回滚的债务已被置0，不计入明细。
        for (Long playerId : activePlayerIds) {
            gameDataVo.recordRoundChange(playerId, roundChanges.getOrDefault(playerId, 0L));
        }
        log.info("斗仙牌本回合实际输赢 round:{} changes:{}", round, roundChanges);
        // 结算已经发生，此时把所有玩家的完整牌面一起带上，前端可以直接渲染亮牌动画，
        // 不用再从pairResults的两两对比数据里反推每个人到底摆了什么牌(selfView=true，
        // 这里是广播给所有人的"结算后"快照，跟摆牌阶段"只有自己能看全"的隐藏规则不冲突，见DESIGN.md 8.8)
        List<DouXianRevealInfo> playerReveals = new ArrayList<>();
        for (Long playerId : activePlayerIds) {
            DouXianRevealInfo reveal = new DouXianRevealInfo();
            reveal.playerId = playerId;
            reveal.zones = DouXianBuilder.buildZonePlacements(playerId, gameDataVo, true);
            playerReveals.add(reveal);
        }

        NotifyDouXianSettlement notify = new NotifyDouXianSettlement();
        notify.round = round;
        notify.playerReveals = playerReveals;
        notify.pairResults = pairResults;
        notify.playerInfos = gameDataVo.getSeatInfo().values().stream()
                .filter(seatInfo -> seatInfo.isSeatDown() && activePlayerIds.contains(seatInfo.getPlayerId()))
                .map(seatInfo -> PokerBuilder.getPokerPlayerInfo(seatInfo, controller))
                .toList();
        broadcastMsgToRoom(notify);

        List<DouXianSpecialRuleInfo> specialRules = round < DouXianConstant.Common.TOTAL_ROUND
                ? detectSpecialRuleTriggers(pairResults) : List.of();
        int specialRuleCount = specialRules.size();
        if (gameController instanceof DouXianGameController douXianController) {
            douXianController.prepareKafkaRoundLog(
                    playerZoneResults, pairResults, specialRules, balanceAfter);
        }
        needRechargePlayerIds = detectNeedRecharge(controller, activePlayerIds);
        boolean waitForClientAck = gameController instanceof DouXianGameController douXianController
                && douXianController.hasOnlineActiveRealPlayer();
        settlementEffectTime = waitForClientAck
                ? DouXianConstant.Time.SETTLEMENT_ANIMATION_ACK_TIMEOUT
                : DouXianConstant.Time.SETTLEMENT_NO_ONLINE_CLIENT_WAIT_TIME;
        super.phaseDoAction();
        log.info("斗仙牌结算表现等待 roomCfgId:{} round:{} openZoneCount:{} specialRuleCount:{} waitTime:{}ms",
                gameDataVo.getRoomCfg().getId(), round, openZones.size(), specialRuleCount, settlementEffectTime);
    }

    private int calculateSettlementEffectTime(int openZoneCount,
                                              List<DouXianPairSettlementInfo> pairResults,
                                              int specialRuleCount) {
        int zoneEffectTime = openZoneCount * DouXianConstant.Time.SETTLEMENT_ZONE_EFFECT_TIME;
        boolean hasGrandWin = pairResults.stream().anyMatch(pair -> pair.grandWinPlayerId != 0);
        int grandWinEffectTime = hasGrandWin ? DouXianConstant.Time.SETTLEMENT_GRAND_WIN_EFFECT_TIME : 0;
        int specialRuleEffectTime = specialRuleCount * DouXianConstant.Time.SETTLEMENT_SPECIAL_RULE_EFFECT_TIME;
        return zoneEffectTime
                + grandWinEffectTime
                + specialRuleEffectTime
                + DouXianConstant.Time.SETTLEMENT_EFFECT_BUFFER_TIME;
    }

    private void logPairTheoretical(DouXianPairSettlementInfo pair) {
        StringBuilder sb = new StringBuilder();
        for (DouXianZoneSettlementInfo zr : pair.zoneResults) {
            if (zr.winnerId == 0) {
                sb.append("zone").append(zr.zoneId).append("=平局(").append(zr.winnerAether).append(':').append(zr.loserAether).append(") ");
            } else {
                sb.append("zone").append(zr.zoneId).append(':').append(zr.winnerId).append("赢").append(zr.loserId)
                        .append('(').append(zr.winnerAether).append(':').append(zr.loserAether).append(")理论").append(zr.changeValue).append(' ');
            }
        }
        log.info("斗仙牌结算-理论金额 {} vs {}: {}", pair.playerAId, pair.playerBId, sb);
        if (pair.grandWinPlayerId != 0) {
            long loserId = pair.grandWinPlayerId == pair.playerAId ? pair.playerBId : pair.playerAId;
            StringBuilder grandSb = new StringBuilder();
            for (DouXianZoneSettlementInfo zr : pair.grandWinExtraResults) {
                grandSb.append("zone").append(zr.zoneId).append("理论").append(zr.changeValue).append(' ');
            }
            log.info("斗仙牌结算-全胜触发 {} 全胜 {}，二次结算理论金额: {}", pair.grandWinPlayerId, loserId, grandSb);
        }
    }

    /**
     * 只算理论结算结果(含全胜二次结算)，不做任何封顶、不碰金币
     */
    private DouXianPairSettlementInfo computePairTheoretical(long playerAId, long playerBId,
                                                               Map<Long, Map<DouXianZone, DouXianHandResult>> playerZoneResults,
                                                               List<DouXianZone> openZones, long betBase) {
        DouXianPairSettlementInfo pair = new DouXianPairSettlementInfo();
        pair.playerAId = playerAId;
        pair.playerBId = playerBId;
        pair.zoneResults = new ArrayList<>();
        Map<DouXianZone, DouXianHandResult> resultsA = playerZoneResults.get(playerAId);
        Map<DouXianZone, DouXianHandResult> resultsB = playerZoneResults.get(playerBId);

        for (DouXianZone zone : openZones) {
            pair.zoneResults.add(DouXianSettlementCalculator.computeZoneResult(
                    playerAId, resultsA.get(zone), playerBId, resultsB.get(zone), betBase));
        }

        if (openZones.size() == 3) {
            if (DouXianHandEvaluator.isGrandWin(resultsA, resultsB)) {
                pair.grandWinPlayerId = playerAId;
                pair.grandWinExtraResults = DouXianSettlementCalculator.computeGrandWinExtra(
                        playerAId, playerBId, resultsA, resultsB, openZones, betBase);
            } else if (DouXianHandEvaluator.isGrandWin(resultsB, resultsA)) {
                pair.grandWinPlayerId = playerBId;
                pair.grandWinExtraResults = DouXianSettlementCalculator.computeGrandWinExtra(
                        playerBId, playerAId, resultsB, resultsA, openZones, betBase);
            }
        }
        return pair;
    }

    private boolean applyTransfer(BasePokerGameController<DouXianGameDataVo> controller,
                                  long winnerId, long loserId, long changeValue) {
        if (changeValue <= 0) {
            return true;
        }
        if (controller instanceof DouXianGameController douXianController) {
            return douXianController.transferSettlementItem(winnerId, loserId, changeValue);
        }
        int deductCode = controller.deductItem(loserId, changeValue, AddType.GAME_SETTLEMENT);
        if (deductCode != Code.SUCCESS) {
            return false;
        }
        int addCode = controller.addItem(winnerId, changeValue, AddType.GAME_SETTLEMENT);
        if (addCode == Code.SUCCESS) {
            return true;
        }
        controller.addItem(loserId, changeValue, AddType.FAIL_ROLLBACK);
        return false;
    }

    /**
     * 统计每个玩家本回合全胜了几个人、被几个人全胜，达到2人触发得证大道/隐忍渡劫。DESIGN.md 三
     */
    private List<DouXianSpecialRuleInfo> detectSpecialRuleTriggers(List<DouXianPairSettlementInfo> pairResults) {
        Map<Long, Integer> grandWinAsWinner = new HashMap<>();
        Map<Long, Integer> grandWinAsLoser = new HashMap<>();
        for (DouXianPairSettlementInfo pair : pairResults) {
            if (pair.grandWinPlayerId == 0) {
                continue;
            }
            long loserId = pair.grandWinPlayerId == pair.playerAId ? pair.playerBId : pair.playerAId;
            grandWinAsWinner.merge(pair.grandWinPlayerId, 1, Integer::sum);
            grandWinAsLoser.merge(loserId, 1, Integer::sum);
        }
        List<DouXianSpecialRuleInfo> ruleInfos = new ArrayList<>();
        int triggerCount = DouXianConstant.Common.SPECIAL_RULE_TRIGGER_PLAYER_COUNT;
        for (Long playerId : gameDataVo.getActivePlayerIds()) {
            int winCount = grandWinAsWinner.getOrDefault(playerId, 0);
            int loseCount = grandWinAsLoser.getOrDefault(playerId, 0);
            Integer forcedRuleType = gameDataVo.getGmForcedSpecialRule().get(playerId);
            DouXianSpecialRuleInfo info = null;
            if (forcedRuleType != null && forcedRuleType == 1) {
                gameDataVo.getPendingSpecialRule().put(playerId, 1);
                info = new DouXianSpecialRuleInfo();
                info.ruleType = 1;
                info.triggerPlayerCount = Math.max(winCount, triggerCount);
            } else if (forcedRuleType != null && forcedRuleType == 2) {
                gameDataVo.getPendingSpecialRule().put(playerId, 2);
                info = new DouXianSpecialRuleInfo();
                info.ruleType = 2;
                info.triggerPlayerCount = Math.max(loseCount, triggerCount);
            } else if (winCount >= triggerCount) {
                gameDataVo.getPendingSpecialRule().put(playerId, 1);
                info = new DouXianSpecialRuleInfo();
                info.ruleType = 1;
                info.triggerPlayerCount = winCount;
            } else if (loseCount >= triggerCount) {
                gameDataVo.getPendingSpecialRule().put(playerId, 2);
                info = new DouXianSpecialRuleInfo();
                info.ruleType = 2;
                info.triggerPlayerCount = loseCount;
            }
            if (info != null) {
                info.playerId = playerId;
                ruleInfos.add(info);
                log.info("斗仙牌特殊规则触发 playerId:{} ruleType:{} triggerCount:{}", playerId, info.ruleType, info.triggerPlayerCount);
            }
        }
        gameDataVo.getGmForcedSpecialRule().clear();
        if (!ruleInfos.isEmpty()) {
            NotifyDouXianSpecialRuleTrigger notify = new NotifyDouXianSpecialRuleTrigger();
            notify.ruleInfos = ruleInfos;
            broadcastMsgToRoom(notify);
        }
        return ruleInfos;
    }

    /**
     * 检测到金币结算至0的玩家，进入30s复活倒计时；花钻石换金币的真正兑换逻辑见
     * {@link com.jjg.game.poker.game.douxian.room.DouXianGameController#reqRecharge}(阶段13已实现，跟支付网关无关)。
     */
    private List<Long> detectNeedRecharge(BasePokerGameController<DouXianGameDataVo> controller, List<Long> activePlayerIds) {
        List<Long> result = new ArrayList<>();
        for (Long playerId : activePlayerIds) {
            if (controller.getTransactionItemNum(playerId) <= 0) {
                result.add(playerId);
            }
        }
        return result;
    }

    /**
     * The first valid real-client acknowledgement releases the barrier. The timer calls the same
     * idempotent path as a fallback, so duplicate acknowledgements and stale timer callbacks are safe.
     */
    public void completeByClient(long playerId, int round) {
        if (!(gameController instanceof DouXianGameController controller)
                || finished || !controller.isCurrentPhase(this)) {
            return;
        }
        controller.removePokerPhaseTimer();
        finishOnce(controller, "clientAck", playerId, round);
    }

    @Override
    public void phaseFinish() {
        if (!(gameController instanceof DouXianGameController controller)
                || !controller.isCurrentPhase(this)) {
            return;
        }
        finishOnce(controller, "timeout", 0L, gameDataVo.getRound());
    }

    private void finishOnce(DouXianGameController controller, String reason, long playerId, int round) {
        if (finished) {
            return;
        }
        finished = true;
        log.info("DouXian settlement presentation barrier released. reason:{} playerId:{} round:{} roomId:{}",
                reason, playerId, round, controller.getRoom().getId());
        if (gameDataVo.getRound() >= DouXianConstant.Common.TOTAL_ROUND) {
            controller.finishRoundCycle();
            return;
        }
        if (!needRechargePlayerIds.isEmpty()) {
            gameDataVo.getRechargingPlayerIds().addAll(needRechargePlayerIds);
            controller.addPokerPhaseTimer(new DouXianRechargePhase(controller));
        } else {
            controller.addPokerPhaseTimer(new DouXianTierAdvancePhase(controller));
        }
    }
    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {
    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {
    }
}
