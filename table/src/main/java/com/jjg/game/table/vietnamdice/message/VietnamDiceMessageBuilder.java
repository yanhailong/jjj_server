package com.jjg.game.table.vietnamdice.message;

import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.vietnamdice.VietnamDiceGameController;
import com.jjg.game.table.vietnamdice.data.VietnamDiceGameDataVo;

/**
 * 越南色碟
 *
 * @author 2CL
 */
public class VietnamDiceMessageBuilder {

    /**
     * 构建结算信息体
     */
    private static VietnamDiceSettlementInfo buildVietnamDiceSettlementInfo(VietnamDiceHistoryBean vietnamDiceHistoryBean) {
        VietnamDiceSettlementInfo vietnamDiceSettlementInfo = new VietnamDiceSettlementInfo();
        vietnamDiceSettlementInfo.rewardAreaIdx = vietnamDiceHistoryBean.betIdxId;
        vietnamDiceSettlementInfo.diceData = vietnamDiceHistoryBean.diceData;
        return vietnamDiceSettlementInfo;
    }

    /**
     * 结算信息
     */
    public static NotifyVietnamDiceSettlement notifyVietnamDiceSettlement(
        VietnamDiceHistoryBean vietnamDiceHistoryBean) {
        NotifyVietnamDiceSettlement settlement = new NotifyVietnamDiceSettlement();
        settlement.settlementInfo = buildVietnamDiceSettlementInfo(vietnamDiceHistoryBean);
        return settlement;
    }

    /**
     * 桌面信息
     */
    public static NotifyVietnamDiceTableInfo notifyVietnamDiceTableInfo(
        long playerId, BaseTableGameController<VietnamDiceGameDataVo> gameController, boolean isInitial) {
        NotifyVietnamDiceTableInfo tableInfo = new NotifyVietnamDiceTableInfo();
        VietnamDiceGameDataVo gameDataVo = gameController.getGameDataVo();
        tableInfo.baseDiceTableInfo =
            BaseDiceMessageBuilder.buildDiceTableInfo(playerId, gameController, gameDataVo, isInitial);
        tableInfo.historyDiceData =
            gameDataVo.getWinAreaCfgIdHistory().stream().map(v -> v.diceData).toList();
        tableInfo.settlementInfo = gameDataVo.getVietnamDiceSettlementInfo();
        return tableInfo;
    }
}
