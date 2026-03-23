package com.jjg.game.table.russianlette.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.dicecommon.phase.BaseDiceSettlementPhase;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.message.RussianLetteMessageBuilder;
import com.jjg.game.table.russianlette.message.resp.NotifyRussianLetteSettlement;
import com.jjg.game.table.russianlette.message.resp.RussianLetteHistoryBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 俄罗斯转盘结算阶段（GAME_ROUND_OVER_SETTLEMENT）
 * <p>
 * 持续 {@code stageTime[3]} 秒（默认 5s，倒计时 4→0）。
 * <p>
 * 前提：开奖阶段（{@link RussianLetteDrawPhase}）已将骰子点数、中奖配置等数据缓存到
 * {@link RussianLetteGameDataVo}，本阶段直接读取缓存进行金币结算。
 * <p>
 * 职责：
 * <ol>
 *   <li>广播 {@code NotifyRussianLettePhaseChangInfo(GAME_ROUND_OVER_SETTLEMENT)} 通知客户端进入结算 UI</li>
 *   <li>读取开奖阶段缓存的 {@code drawPhaseHistoryBean} 和 {@code drawPhaseWinCfgs}</li>
 *   <li>通过 {@code settlementDice()} 完成所有玩家金币结算并广播 {@link NotifyRussianLetteSettlement}</li>
 *   <li>将最终结算信息保存到 {@code gameDataVo.setSettlementInfo()}，供断线重连使用</li>
 * </ol>
 *
 * @author lhc
 */
public class RussianLetteSettlementPhase extends BaseDiceSettlementPhase<RussianLetteGameDataVo> {

    public RussianLetteSettlementPhase(AbstractPhaseGameController<Room_BetCfg, RussianLetteGameDataVo> gameController) {
        super(gameController);
    }

    /**
     * 结算阶段持续时间：stageTime[2]（默认 5s）
     * <p>
     * 基类 {@code BaseSettlementPhase.getPhaseRunTime()} 返回 stageTime[2]+stageTime[3]，
     * 这里覆盖为只取 stageTime[2]，因为 stageTime[2] 已由 {@link RussianLetteDrawPhase} 使用。
     */
    @Override
    public int getPhaseRunTime() {
        List<Integer> stageTime = gameDataVo.getRoomCfg().getStageTime();
        if (stageTime.size() >= 3) {
            return stageTime.get(2);
        }
        return 5000;
    }

    /**
     * 阶段开始：
     * 1. 读取开奖阶段缓存数据
     * 2. 构建结算消息体
     * 3. 逐玩家计算金币结算（含 playerBetGold）
     * 4. 逐玩家广播结算消息（含 playNum、allBet）
     * 5. 数据埋点 & 保存结算信息
     * 6. 通知观察者
     */
    @Override
    public void phaseDoAction() {
        long startTime = System.currentTimeMillis();
        super.phaseDoAction();
//        log.info("执行RussianLetteSettlementPhase（结算阶段）中phaseDoAction");

        // ── 1. 读取开奖阶段缓存 ──────────────────────────────────────────────────
        RussianLetteHistoryBean historyBean = gameDataVo.getDrawPhaseHistoryBean();
        List<WinPosWeightCfg> winPosWeightCfgs = gameDataVo.getDrawPhaseWinCfgs();
        if (historyBean == null || winPosWeightCfgs == null || winPosWeightCfgs.isEmpty()) {
            log.error("俄罗斯转盘结算阶段异常：开奖阶段缓存数据为空 room:{}", gameDataVo.roomLogInfo());
            return;
        }

        // ── 2. 构建结算消息体（含 stageInfo、prob）─────────────────────────────
        NotifyRussianLetteSettlement settlement =
                RussianLetteMessageBuilder.notifyAnimalsSettlement(historyBean, gameDataVo);
        settlement.settlementInfo.diceSettlementInfo =
                BaseDiceMessageBuilder.buildDiceSettlementInfo(gameDataVo);

        // ── 3. 逐玩家计算金币结算（借鉴百家乐，补全 playerBetGold）──────────────
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>();
        RoomBankerChangeParam changeParam = getRoomBankerChangeParam(gameDataVo.getRealPlayerAreaBetInfo());
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();

        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            GamePlayer gamePlayer = entry.getValue();
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
            if (playerBetInfo == null || playerBetInfo.isEmpty()) {
                continue;
            }

            // 计算玩家本局下注总金额
            long playerTotalBetGold = playerBetInfo.values().stream()
                    .map(a -> a.stream().mapToInt(Integer::intValue).sum())
                    .mapToLong(Integer::longValue)
                    .sum();

            // 计算结算金币
            SettlementData playerSettlementData =
                    calcSettlementGold(gamePlayer, winPosWeightCfgs, playerBetInfo, changeParam);

            PlayerChangedGold playerChangedGold = new PlayerChangedGold();
            playerChangedGold.playerId = playerId;
            playerChangedGold.playerWinGold = playerSettlementData.getTotalWin();
            playerChangedGold.playerBetGold = playerTotalBetGold;

            // 赢了才加金币
            long totalWin = playerSettlementData.getTotalWin();
            if (totalWin > 0) {
                int addCode = gameController.addItem(gamePlayer.getId(), totalWin,
                        AddType.GAME_SETTLEMENT, gameDataVo.getRoomCfg().getId() + "");
                if (addCode != Code.SUCCESS) {
                    log.error("俄罗斯转盘结算给玩家加金币失败 playerId:{} totalWin:{} code:{}",
                            gamePlayer.getId(), totalWin, addCode);
                }
            }
            playerChangedGold.playerCurGold = gameController.getTransactionItemNum(gamePlayer.getId());

            // playerWinGold = 0 的不同步到结算消息
            if (playerChangedGold.playerWinGold != 0) {
                playerChangedGolds.add(playerChangedGold);
            }

            // 添加下注记录
            entry.getValue().getTableGameData().addBetRecord(playerSettlementData.getTotalWin());

            if (changeParam != null && !(gamePlayer instanceof GameRobotPlayer)) {
                changeParam.addBankerChangeGold(
                        Math.max(0, playerSettlementData.getTotalGet() - playerSettlementData.getBankerWind()));
                changeParam.addTotalTaxRevenue(playerSettlementData.getTaxation());
            }
            settlementDataMap.put(playerId, playerSettlementData);
        }

        if (changeParam != null) {
            calculationFinalBankerChange(changeParam);
            dealRoomPool(changeParam);
            gameController.dealBankerFlowing(changeParam, settlementDataMap);
        }

        // 设置玩家金币变化列表
        settlement.settlementInfo.diceSettlementInfo.playerChangedGolds = playerChangedGolds;

        // ── 4. 逐玩家广播结算消息（填充玩家维度的 playNum、allBet）──────────────
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            addPlayerAreaDataLog(entry.getValue());

            // 玩家牌桌玩的次数
            settlement.playNum = entry.getValue().getTableGameData().getPlayNum();

            // 累计本局下注总金额
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
            if (playerBetInfo != null) {
                settlement.allBet = playerBetInfo.values().stream()
                        .flatMapToInt(list -> list.stream().mapToInt(Integer::intValue))
                        .sum();
            } else {
                settlement.allBet = 0;
            }

            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder()
                    .setData(settlement).addPlayerId(playerId));
        }

        log.debug("俄罗斯转盘 {} 结算完成：{}", gameDataVo.roomLogInfo(), JSON.toJSONString(settlement));

        // ── 5. 追加数据埋点 ──────────────────────────────────────────────────────
        gameDataTracker.addGameLogData(DataTrackNameConstant.SETTLEMENT_DATA, historyBean);

        // ── 6. 保存最终结算信息（含金币变化），供断线重连恢复 ─────────────────────
        gameDataVo.setSettlementInfo(settlement.settlementInfo);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);

        // ── 7. 通知所有观察者（房间列表页玩家）──────────────────────────────────
        RussianLetteMessageBuilder.notifyObserversOnPhaseChange(
                (BaseTableGameController<RussianLetteGameDataVo>) gameController);

        long endTime = System.currentTimeMillis();
//        log.info("结算阶段  执行时间：{}   距离下个阶段时间：{}",endTime-startTime,gameDataVo.getPhaseEndTime()-endTime);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
