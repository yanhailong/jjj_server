package com.jjg.game.table.riveranimals.message;

import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.riveranimals.RiverAnimalsGameController;
import com.jjg.game.table.riveranimals.data.RiverAnimalsGameDataVo;

/**
 * 鱼虾蟹
 *
 * @author 2CL
 */
public class RiverAnimalsMessageBuilder {

    /**
     * 构建结算信息体
     */
    private static RiverAnimalsSettlementInfo buildAnimalsSettlementInfo(
        RiverAnimalsHistoryBean riverAnimalsHistoryBean) {
        RiverAnimalsSettlementInfo riverAnimalsSettlementInfo = new RiverAnimalsSettlementInfo();
        riverAnimalsSettlementInfo.rewardAreaIdx = riverAnimalsHistoryBean.betIdxId;
        riverAnimalsSettlementInfo.diceData = riverAnimalsHistoryBean.diceData;
        return riverAnimalsSettlementInfo;
    }

    /**
     * 结算信息
     */
    public static NotifyRiverAnimalsSettlement notifyAnimalsSettlement(
        RiverAnimalsHistoryBean riverAnimalsHistoryBean) {
        NotifyRiverAnimalsSettlement settlement = new NotifyRiverAnimalsSettlement();
        settlement.settlementInfo = buildAnimalsSettlementInfo(riverAnimalsHistoryBean);
        return settlement;
    }

    /**
     * 桌面信息
     */
    public static NotifyRiverAnimalsTableInfo notifyAnimalsTableInfo(
        long playerId, RiverAnimalsGameController gameController, boolean isInitial) {
        NotifyRiverAnimalsTableInfo tableInfo = new NotifyRiverAnimalsTableInfo();
        RiverAnimalsGameDataVo gameDataVo = gameController.getGameDataVo();
        tableInfo.baseDiceTableInfo =
            BaseDiceMessageBuilder.buildDiceTableInfo(
                playerId, gameController.getCurrentGamePhase(), gameDataVo, isInitial);
        tableInfo.settlementHistory = gameDataVo.getWinAreaCfgIdHistory();
        tableInfo.settlementInfo = gameDataVo.getAnimalsSettlementInfo();
        return tableInfo;
    }
}
