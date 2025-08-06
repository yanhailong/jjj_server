package com.jjg.game.table.riveranimals.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.betsample.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.dicecommon.DiceDataHolder;
import com.jjg.game.table.dicecommon.DiceUtils;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.dicecommon.phase.BaseDiceSettlementPhase;
import com.jjg.game.table.riveranimals.data.RiverAnimalsGameDataVo;
import com.jjg.game.table.riveranimals.message.NotifyRiverAnimalsSettlement;
import com.jjg.game.table.riveranimals.message.RiverAnimalsHistoryBean;
import com.jjg.game.table.riveranimals.message.RiverAnimalsMessageBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 鱼虾蟹结算
 *
 * @author 2CL
 */
public class RiverAnimalsSettlementPhase extends BaseDiceSettlementPhase<RiverAnimalsGameDataVo> {

    public RiverAnimalsSettlementPhase(AbstractPhaseGameController<Room_BetCfg, RiverAnimalsGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 随机四个1-2的骰子点数
        List<Integer> randomNumDice = DiceUtils.randomDice(4, 1, 2);
        // 通过骰子点数获取对应的配置
        List<WinPosWeightCfg> winPosWeightCfgs =
            DiceDataHolder.getWinPosWeightCfg(EGameType.VIETNAM_DICE, randomNumDice);
        if (winPosWeightCfgs == null || winPosWeightCfgs.isEmpty()) {
            log.error("鱼虾蟹结算异常，随机奖励的区域为空，骰子：{}", randomNumDice);
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
        log.info("{} 摇中鱼虾蟹：{}, 区域ID: {} 对应的中奖区域：{}",
            gameDataVo.roomLogInfo(),
            randomNumDice.stream().map(String::valueOf).collect(Collectors.joining("")),
            winPosWeightCfgs.stream().map(WinPosWeightCfg::getId).collect(Collectors.toList()),
            betAreaCfgs.stream().map(BetAreaCfg::getId).map(String::valueOf).collect(Collectors.joining(",")));
        // 添加中奖记录
        RiverAnimalsHistoryBean historyBean = addHistory(randomNumDice, betAreaCfgs);
        NotifyRiverAnimalsSettlement settlement =
            RiverAnimalsMessageBuilder.notifyAnimalsSettlement(historyBean);
        // 添加历史
        gameDataVo.addWinAreaCfgIdHistory(historyBean);
        // 构建结算信息
        settlement.settlementInfo.diceSettlementInfo =
            BaseDiceMessageBuilder.buildDiceSettlementInfo(gameDataVo);
        // 通用骰子结算逻辑
        settlementDice(settlement.settlementInfo.diceSettlementInfo, winPosWeightCfgs, settlement);
        log.debug("鱼虾蟹房间：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setAnimalsSettlementInfo(settlement.settlementInfo);
    }

    /**
     * 添加鱼虾蟹的中奖历史记录
     */
    private RiverAnimalsHistoryBean addHistory(
        List<Integer> diceData, List<BetAreaCfg> winPosWeightCfgs) {
        RiverAnimalsHistoryBean riverAnimalsHistoryBean = new RiverAnimalsHistoryBean();
        riverAnimalsHistoryBean.betIdxId = winPosWeightCfgs.stream().map(BetAreaCfg::getId).toList();
        riverAnimalsHistoryBean.diceData = diceData;
        // 添加记录
        gameDataVo.addWinAreaCfgIdHistory(riverAnimalsHistoryBean);
        return riverAnimalsHistoryBean;
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
