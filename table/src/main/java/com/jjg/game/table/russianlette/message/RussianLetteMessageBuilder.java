package com.jjg.game.table.russianlette.message;

import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;

/**
 * 俄罗斯转盘
 *
 * @author lhc
 */
public class RussianLetteMessageBuilder {

    /**
     * 构建结算信息体
     */
    private static RussianLetteSettlementInfo buildAnimalsSettlementInfo(
        RussianLetteHistoryBean russianLetteHistoryBean) {
        RussianLetteSettlementInfo russianLetteSettlementInfo = new RussianLetteSettlementInfo();
        russianLetteSettlementInfo.rewardAreaIdx = russianLetteHistoryBean.betIdxId;
        russianLetteSettlementInfo.diceData = russianLetteHistoryBean.diceData;
        return russianLetteSettlementInfo;
    }

    /**
     * 结算信息
     */
    public static NotifyRussianLetteSettlement notifyAnimalsSettlement(
        RussianLetteHistoryBean russianLetteHistoryBean) {
        NotifyRussianLetteSettlement settlement = new NotifyRussianLetteSettlement();
        settlement.settlementInfo = buildAnimalsSettlementInfo(russianLetteHistoryBean);
        return settlement;
    }

    /**
     * 桌面信息
     */
    public static NotifyRussianLetteTableInfo notifyAnimalsTableInfo(
            long playerId, BaseTableGameController<RussianLetteGameDataVo> gameController, boolean isInitial) {
        NotifyRussianLetteTableInfo tableInfo = new NotifyRussianLetteTableInfo();
        RussianLetteGameDataVo gameDataVo = gameController.getGameDataVo();
        tableInfo.baseDiceTableInfo =
            BaseDiceMessageBuilder.buildDiceTableInfo(playerId, gameController, gameDataVo, isInitial);
        tableInfo.settlementHistory = gameDataVo.getWinAreaCfgIdHistory();
        tableInfo.settlementInfo = gameDataVo.getAnimalsSettlementInfo();
        return tableInfo;
    }
}
