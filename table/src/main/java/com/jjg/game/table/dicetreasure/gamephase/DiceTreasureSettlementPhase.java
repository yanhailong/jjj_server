package com.jjg.game.table.dicetreasure.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.betsample.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.common.utils.WinPosWeightUtils;
import com.jjg.game.table.dicecommon.DiceDataHolder;
import com.jjg.game.table.dicecommon.DiceUtils;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.dicecommon.phase.BaseDiceSettlementPhase;
import com.jjg.game.table.dicetreasure.DiceTreasureDiceGameController;
import com.jjg.game.table.dicetreasure.data.DiceTreasureGameDataVo;
import com.jjg.game.table.dicetreasure.message.NotifyDiceTreasureSettlement;
import com.jjg.game.table.dicetreasure.message.DiceTreasureHistoryBean;
import com.jjg.game.table.dicetreasure.message.DiceTreasureMessageBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 骰宝结算
 *
 * @author 2CL
 */
public class DiceTreasureSettlementPhase extends BaseDiceSettlementPhase<DiceTreasureGameDataVo> {

    public DiceTreasureSettlementPhase(AbstractPhaseGameController<Room_BetCfg, DiceTreasureGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 随机三个1-6的骰子点数
        List<Integer> randomNumDice = DiceUtils.randomDice(3, 1, 6);
        // 通过骰子点数获取对应的配置
        List<WinPosWeightCfg> winPosWeightCfgs =
            DiceDataHolder.getWinPosWeightCfg(EGameType.DICE_TREASURE, randomNumDice);
        if (winPosWeightCfgs == null || winPosWeightCfgs.isEmpty()) {
            log.error("骰宝结算异常，随机奖励的区域为空，骰子：{}", randomNumDice);
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
        log.debug("{} 摇中骰子：{}, 区域ID: {} 对应的中奖区域：{}",
            gameDataVo.roomLogInfo(), randomNumDice.stream().map(String::valueOf).collect(Collectors.joining("")),
            winPosWeightCfgs.stream().map(WinPosWeightCfg::getId).collect(Collectors.toList()),
            betAreaCfgs.stream().map(BetAreaCfg::getId).map(String::valueOf).collect(Collectors.joining(",")));
        // 添加中奖记录
        DiceTreasureHistoryBean historyBean = addDiceTreasureHistory(randomNumDice, betAreaCfgs);
        NotifyDiceTreasureSettlement settlement =
            DiceTreasureMessageBuilder.notifyDiceTreasureSettlement(historyBean);
        gameDataTracker.addGameLogData(DataTrackNameConstant.SETTLEMENT_DATA, historyBean);
        // 构建结算信息
        settlement.settlementInfo.diceSettlementInfo =
            BaseDiceMessageBuilder.buildDiceSettlementInfo(gameDataVo);
        settlementDice(settlement.settlementInfo.diceSettlementInfo, winPosWeightCfgs, settlement);
        log.debug("骰宝房间：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setAnimalsSettlementInfo(settlement.settlementInfo);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }

    /**
     * 添加骰宝的中奖历史记录
     */
    private DiceTreasureHistoryBean addDiceTreasureHistory(List<Integer> randomNumDice, List<BetAreaCfg> betAreaCfgs) {
        DiceTreasureHistoryBean diceTreasureHistoryBean = new DiceTreasureHistoryBean();
        diceTreasureHistoryBean.diceList = randomNumDice;
        diceTreasureHistoryBean.betIdxId = betAreaCfgs.stream().map(BetAreaCfg::getId).collect(Collectors.toList());
        // 添加记录
        gameDataVo.addWinAreaCfgIdHistory(diceTreasureHistoryBean);
        return diceTreasureHistoryBean;
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
