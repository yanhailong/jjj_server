package com.jjg.game.table.sizedicetreasure.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.betsample.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.dicecommon.DiceDataHolder;
import com.jjg.game.table.dicecommon.DiceUtils;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.dicecommon.phase.BaseDiceSettlementPhase;
import com.jjg.game.table.sizedicetreasure.data.SizeDiceTreasureGameDataVo;
import com.jjg.game.table.sizedicetreasure.message.NotifySizeDiceTreasureSettlement;
import com.jjg.game.table.sizedicetreasure.message.SizeDiceTreasureHistoryBean;
import com.jjg.game.table.sizedicetreasure.message.SizeDiceTreasureMessageBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 大小骰宝结算
 *
 * @author 2CL
 */
public class SizeDiceTreasureSettlementPhase extends BaseDiceSettlementPhase<SizeDiceTreasureGameDataVo> {

    public SizeDiceTreasureSettlementPhase(AbstractPhaseGameController<Room_BetCfg, SizeDiceTreasureGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 随机四个1-2的骰子点数
        List<Integer> randomNumDice = DiceUtils.randomDice(3, 1, 6);
        // 通过骰子点数获取对应的配置
        List<WinPosWeightCfg> winPosWeightCfgs =
            DiceDataHolder.getWinPosWeightCfg(EGameType.SIZE_DICE_TREASURE, randomNumDice);
        if (winPosWeightCfgs == null || winPosWeightCfgs.isEmpty()) {
            log.error("大小骰宝结算异常，随机奖励的区域为空，骰子：{}", randomNumDice);
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
        log.debug("{} 摇中大小骰宝：{}, 区域ID: {} 对应的中奖区域：{}",
            gameDataVo.roomLogInfo(),
            randomNumDice.stream().map(String::valueOf).collect(Collectors.joining("")),
            winPosWeightCfgs.stream().map(WinPosWeightCfg::getId).collect(Collectors.toList()),
            betAreaCfgs.stream().map(BetAreaCfg::getId).map(String::valueOf).collect(Collectors.joining(",")));
        // 添加中奖记录
        SizeDiceTreasureHistoryBean historyBean = addHistory(randomNumDice, betAreaCfgs);
        gameDataTracker.addGameLogData(DataTrackNameConstant.SETTLEMENT_DATA, historyBean);
        NotifySizeDiceTreasureSettlement settlement =
            SizeDiceTreasureMessageBuilder.notifySizeDiceSettlement(historyBean);
        // 构建结算信息
        settlement.settlementInfo.diceSettlementInfo =
            BaseDiceMessageBuilder.buildDiceSettlementInfo(gameDataVo);
        // 通用骰子结算逻辑
        settlementDice(settlement.settlementInfo.diceSettlementInfo, winPosWeightCfgs, settlement);
        log.debug("大小骰宝房间：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setAnimalsSettlementInfo(settlement.settlementInfo);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }

    /**
     * 添加大小骰宝的中奖历史记录
     */
    private SizeDiceTreasureHistoryBean addHistory(
        List<Integer> diceData, List<BetAreaCfg> winPosWeightCfgs) {
        SizeDiceTreasureHistoryBean sizeDiceTreasureHistoryBean = new SizeDiceTreasureHistoryBean();
        sizeDiceTreasureHistoryBean.betIdxId = winPosWeightCfgs.get(0).getId();
        sizeDiceTreasureHistoryBean.diceData = diceData;
        // 添加记录
        gameDataVo.addWinAreaCfgIdHistory(sizeDiceTreasureHistoryBean);
        return sizeDiceTreasureHistoryBean;
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
