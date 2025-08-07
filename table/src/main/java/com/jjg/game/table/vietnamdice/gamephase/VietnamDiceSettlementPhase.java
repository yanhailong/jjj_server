package com.jjg.game.table.vietnamdice.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.BitUtils;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.betsample.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.dicecommon.DiceDataHolder;
import com.jjg.game.table.dicecommon.DiceUtils;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.dicecommon.phase.BaseDiceSettlementPhase;
import com.jjg.game.table.vietnamdice.data.VietnamDiceGameDataVo;
import com.jjg.game.table.vietnamdice.message.NotifyVietnamDiceSettlement;
import com.jjg.game.table.vietnamdice.message.VietnamDiceHistoryBean;
import com.jjg.game.table.vietnamdice.message.VietnamDiceMessageBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 越南色碟结算
 *
 * @author 2CL
 */
public class VietnamDiceSettlementPhase extends BaseDiceSettlementPhase<VietnamDiceGameDataVo> {

    public VietnamDiceSettlementPhase(AbstractPhaseGameController<Room_BetCfg, VietnamDiceGameDataVo> gameController) {
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
            log.error("越南色碟结算异常，随机奖励的区域为空，骰子：{}", randomNumDice);
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
        log.debug("{} 摇中越南骰子：{}, 区域ID: {} 对应的中奖区域：{}",
            gameDataVo.roomLogInfo(),
            randomNumDice.stream().map(dice -> dice == 2 ? "红" : "黑").collect(Collectors.joining("")),
            winPosWeightCfgs.stream().map(WinPosWeightCfg::getId).collect(Collectors.toList()),
            betAreaCfgs.stream().map(BetAreaCfg::getId).map(String::valueOf).collect(Collectors.joining(",")));
        // 添加中奖记录
        VietnamDiceHistoryBean historyBean = addVietnamDiceHistory(randomNumDice, betAreaCfgs);
        NotifyVietnamDiceSettlement settlement =
            VietnamDiceMessageBuilder.notifyVietnamDiceSettlement(historyBean);
        // 添加历史
        gameDataVo.addWinAreaCfgIdHistory(historyBean);
        // 构建结算信息
        settlement.settlementInfo.diceSettlementInfo =
            BaseDiceMessageBuilder.buildDiceSettlementInfo(gameDataVo);
        // 通用骰子结算逻辑
        settlementDice(settlement.settlementInfo.diceSettlementInfo, winPosWeightCfgs, settlement);
        log.debug("越南色碟房间：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setVietnamDiceSettlementInfo(settlement.settlementInfo);
    }

    /**
     * 添加骰宝的中奖历史记录
     */
    private VietnamDiceHistoryBean addVietnamDiceHistory(List<Integer> randomNumDice, List<BetAreaCfg> betAreaCfgs) {
        VietnamDiceHistoryBean vietnamDiceHistoryBean = new VietnamDiceHistoryBean();
        byte diceData = 0;
        for (int i = 0; i < randomNumDice.size(); i++) {
            diceData = randomNumDice.get(i) == 1 ? BitUtils.setBitTrue(diceData, i) : BitUtils.setBitFalse(diceData, i);
        }
        vietnamDiceHistoryBean.diceData = diceData;
        vietnamDiceHistoryBean.betIdxId = betAreaCfgs.stream().map(BetAreaCfg::getId).collect(Collectors.toList());
        // 添加记录
        gameDataVo.addWinAreaCfgIdHistory(vietnamDiceHistoryBean);
        return vietnamDiceHistoryBean;
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
