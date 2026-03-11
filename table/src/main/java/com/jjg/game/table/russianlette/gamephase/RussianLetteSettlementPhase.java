package com.jjg.game.table.russianlette.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.dicecommon.phase.BaseDiceSettlementPhase;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.message.RussianLetteMessageBuilder;
import com.jjg.game.table.russianlette.message.resp.NotifyRussianLetteSettlement;
import com.jjg.game.table.russianlette.message.resp.RussianLetteHistoryBean;
import com.jjg.game.table.russianlette.message.resp.RussianLetteSettlementInfo;

import java.util.List;

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
     * 1. 广播结算阶段变化通知（客户端展示结算面板）
     * 2. 读取开奖阶段缓存数据，执行金币结算并广播结算详情
     */
    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        log.info("执行RussianLetteSettlementPhase（结算阶段）中phaseDoAction");
        // ── 1. 读取开奖阶段缓存 ──────────────────────────────────────────────────
        RussianLetteHistoryBean historyBean = gameDataVo.getDrawPhaseHistoryBean();
        List<WinPosWeightCfg> winPosWeightCfgs = gameDataVo.getDrawPhaseWinCfgs();
        if (historyBean == null || winPosWeightCfgs == null || winPosWeightCfgs.isEmpty()) {
            log.error("俄罗斯转盘结算阶段异常：开奖阶段缓存数据为空 room:{}", gameDataVo.roomLogInfo());
            return;
        }

        // ── 2. 广播结算阶段变化通知（通知客户端切换为结算 UI，携带开奖号码，无金币变化）─
//        RussianLetteSettlementInfo partialSettlementInfo =
//                RussianLetteMessageBuilder.buildSettlementInfoFromHistory(historyBean);
//        broadcastMsgToRoom(
//                RussianLetteMessageBuilder.buildPhaseChangInfo(
//                        EGamePhase.GAME_ROUND_OVER_SETTLEMENT,
//                        gameDataVo.getPhaseEndTime(),
//                        null,
//                        partialSettlementInfo));

        // ── 3. 构建结算消息体（含 stageInfo、prob）─────────────────────────────
        NotifyRussianLetteSettlement settlement =
                RussianLetteMessageBuilder.notifyAnimalsSettlement(historyBean, gameDataVo);
        // 填充玩家下注汇总信息
        settlement.settlementInfo.diceSettlementInfo =
                BaseDiceMessageBuilder.buildDiceSettlementInfo(gameDataVo);

        // ── 4. 执行金币结算并广播（settlementDice 内部向每位玩家广播 settlement 消息）─
        settlementDice(settlement.settlementInfo.diceSettlementInfo, winPosWeightCfgs, settlement);
        log.debug("俄罗斯转盘 {} 结算完成：{}", gameDataVo.roomLogInfo(), JSON.toJSONString(settlement));

        // ── 5. 追加数据埋点 ──────────────────────────────────────────────────────
        gameDataTracker.addGameLogData(DataTrackNameConstant.SETTLEMENT_DATA, historyBean);

        // ── 6. 保存最终结算信息（含金币变化），供断线重连恢复 ─────────────────────
        gameDataVo.setSettlementInfo(settlement.settlementInfo);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);

        // ── 7. 通知所有观察者（房间列表页玩家）──────────────────────────────────
        RussianLetteMessageBuilder.notifyObserversOnPhaseChange(
                (BaseTableGameController<RussianLetteGameDataVo>) gameController);
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
