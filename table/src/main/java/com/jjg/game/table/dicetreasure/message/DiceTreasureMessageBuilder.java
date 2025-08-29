package com.jjg.game.table.dicetreasure.message;

import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.dicetreasure.DiceTreasureGameController;
import com.jjg.game.table.dicetreasure.data.DiceTreasureGameDataVo;

/**
 * 骰宝
 *
 * @author 2CL
 */
public class DiceTreasureMessageBuilder {

    /**
     * 构建结算信息体
     */
    private static DiceTreasureSettlementInfo buildDiceTreasureSettlementInfo(
        DiceTreasureHistoryBean diceTreasureHistoryBean) {
        DiceTreasureSettlementInfo diceTreasureSettlementInfo = new DiceTreasureSettlementInfo();
        diceTreasureSettlementInfo.rewardAreaIdx = diceTreasureHistoryBean.betIdxId;
        diceTreasureSettlementInfo.diceList = diceTreasureHistoryBean.diceList;
        return diceTreasureSettlementInfo;
    }

    /**
     * 结算信息
     */
    public static NotifyDiceTreasureSettlement notifyDiceTreasureSettlement(
        DiceTreasureHistoryBean diceTreasureHistoryBean) {
        NotifyDiceTreasureSettlement settlement = new NotifyDiceTreasureSettlement();
        settlement.settlementInfo = buildDiceTreasureSettlementInfo(diceTreasureHistoryBean);
        return settlement;
    }

    /**
     * 桌面信息
     */
    public static NotifyDiceTreasureTableInfo notifyDiceTreasureTableInfo(
        long playerId, BaseTableGameController<DiceTreasureGameDataVo> gameController, boolean isInitial) {
        NotifyDiceTreasureTableInfo tableInfo = new NotifyDiceTreasureTableInfo();
        DiceTreasureGameDataVo gameDataVo = gameController.getGameDataVo();
        tableInfo.baseDiceTableInfo =
            BaseDiceMessageBuilder.buildDiceTableInfo(
                playerId, gameController.getCurrentGamePhase(), gameDataVo, isInitial);
        tableInfo.settlementInfo = gameDataVo.getAnimalsSettlementInfo();
        tableInfo.settlementHistory = gameDataVo.getWinAreaCfgIdHistory();
        return tableInfo;
    }
}
