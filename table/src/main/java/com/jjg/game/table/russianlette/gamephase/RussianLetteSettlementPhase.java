package com.jjg.game.table.russianlette.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.dicecommon.DiceDataHolder;
import com.jjg.game.table.dicecommon.DiceUtils;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.dicecommon.phase.BaseDiceSettlementPhase;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;

import com.jjg.game.table.russianlette.message.RussianLetteMessageBuilder;
import com.jjg.game.table.russianlette.message.resp.NotifyRussianLetteSettlement;
import com.jjg.game.table.russianlette.message.resp.RussianLetteHistoryBean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 俄罗斯转盘结算
 *
 * @author lhc
 */
public class RussianLetteSettlementPhase extends BaseDiceSettlementPhase<RussianLetteGameDataVo> {

    public RussianLetteSettlementPhase(AbstractPhaseGameController<Room_BetCfg, RussianLetteGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 随机1个0-37的骰子点数
        List<Integer> randomNumDice = null;
        Pair<Long, Long> currentPool = canTriggerRecycling();
        if (currentPool != null) {
            List<Integer> result = generateRecyclingResults(1, 0, 37, EGameType.RUSSIAN_ROULETTE);
            if (result == null) {
                log.error("俄罗斯转盘回收触发 生成结果失败 当前池:{} 标准池:{}", currentPool.getFirst(), currentPool.getSecond());
            } else {
                randomNumDice = result;
                log.info("俄罗斯转盘回收触发 生成结果成功 当前池:{} 标准池:{}", currentPool.getFirst(), currentPool.getSecond());
            }
        }
        if (randomNumDice == null) {
            randomNumDice = DiceUtils.randomDice(1, 0, 37);
        }
        int diceDate = randomNumDice.getFirst();
        // 通过骰子点数获取对应的配置
        List<WinPosWeightCfg> winPosWeightCfgs = DiceDataHolder.getWinPosWeightCfg(EGameType.RUSSIAN_ROULETTE, randomNumDice);
        if (winPosWeightCfgs == null || winPosWeightCfgs.isEmpty()) {
            log.error("俄罗斯转盘结算异常，随机奖励的区域为空，骰子：{}", randomNumDice);
            return;
        }
        // 中奖的区域列表
        List<BetAreaCfg> betAreaCfgs =
                winPosWeightCfgs.stream()
                        .map(WinPosWeightCfg::getBetArea)
                        .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll)
                        .stream()
                        .distinct()
                        .map(a -> GameDataManager.getBetAreaCfg((Integer) a)).toList();
        log.debug("{} 摇中俄罗斯转盘：{}, 区域ID: {} 对应的中奖区域：{}",
                gameDataVo.roomLogInfo(),
                diceDate,
                winPosWeightCfgs.stream().map(WinPosWeightCfg::getId).collect(Collectors.toList()),
                betAreaCfgs.stream().map(BetAreaCfg::getId).map(String::valueOf).collect(Collectors.joining(",")));
        // 添加中奖记录
        RussianLetteHistoryBean historyBean = addHistory(diceDate, betAreaCfgs);
        gameDataTracker.addGameLogData(DataTrackNameConstant.SETTLEMENT_DATA, historyBean);
        NotifyRussianLetteSettlement settlement =
                RussianLetteMessageBuilder.notifyAnimalsSettlement(historyBean);
        // 构建结算信息
        settlement.settlementInfo.diceSettlementInfo =
                BaseDiceMessageBuilder.buildDiceSettlementInfo(gameDataVo);
        // 通用骰子结算逻辑
        settlementDice(settlement.settlementInfo.diceSettlementInfo, winPosWeightCfgs, settlement);
        log.debug("俄罗斯转盘房间：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setSettlementInfo(settlement.settlementInfo);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }

    /**
     * 添加俄罗斯转盘的中奖历史记录
     */
    private RussianLetteHistoryBean addHistory(
            int diceData, List<BetAreaCfg> winPosWeightCfgs) {
        RussianLetteHistoryBean russianLetteHistoryBean = new RussianLetteHistoryBean();
        russianLetteHistoryBean.betIdxId = winPosWeightCfgs.stream().map(BetAreaCfg::getId).toList();
        russianLetteHistoryBean.diceData = diceData;
        // 添加记录
        gameDataVo.addWinAreaCfgIdHistory(russianLetteHistoryBean);
        return russianLetteHistoryBean;
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
