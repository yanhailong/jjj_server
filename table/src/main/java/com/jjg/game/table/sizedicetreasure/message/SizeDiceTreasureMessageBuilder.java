package com.jjg.game.table.sizedicetreasure.message;

import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.common.TableConstant;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.sizedicetreasure.SizeDiceTreasureGameController;
import com.jjg.game.table.sizedicetreasure.data.SizeDiceTreasureGameDataVo;

import java.util.Collections;
import java.util.List;

/**
 * 大小骰宝
 *
 * @author 2CL
 */
public class SizeDiceTreasureMessageBuilder {

    /**
     * 构建结算信息体
     */
    private static SizeDiceTreasureSettlementInfo buildAnimalsSettlementInfo(
        SizeDiceTreasureHistoryBean sizeDiceTreasureHistoryBean) {
        SizeDiceTreasureSettlementInfo sizeDiceTreasureSettlementInfo = new SizeDiceTreasureSettlementInfo();
        sizeDiceTreasureSettlementInfo.rewardAreaIdx = sizeDiceTreasureHistoryBean.betIdxId;
        sizeDiceTreasureSettlementInfo.diceData = sizeDiceTreasureHistoryBean.diceData;
        return sizeDiceTreasureSettlementInfo;
    }

    /**
     * 结算信息
     */
    public static NotifySizeDiceTreasureSettlement notifySizeDiceSettlement(
        SizeDiceTreasureHistoryBean sizeDiceTreasureHistoryBean) {
        NotifySizeDiceTreasureSettlement settlement = new NotifySizeDiceTreasureSettlement();
        settlement.settlementInfo = buildAnimalsSettlementInfo(sizeDiceTreasureHistoryBean);
        return settlement;
    }

    /**
     * 桌面信息
     */
    public static NotifySizeDiceTreasureTableInfo notifyAnimalsTableInfo(
        long playerId, SizeDiceTreasureGameController gameController, boolean isInitial) {
        NotifySizeDiceTreasureTableInfo tableInfo = new NotifySizeDiceTreasureTableInfo();
        SizeDiceTreasureGameDataVo gameDataVo = gameController.getGameDataVo();
        tableInfo.baseDiceTableInfo =
            BaseDiceMessageBuilder.buildDiceTableInfo(
                playerId, gameController.getCurrentGamePhase(), gameDataVo, isInitial);
        tableInfo.settlementInfo = gameDataVo.getAnimalsSettlementInfo();
        tableInfo.settlementHistory = gameDataVo.getWinAreaCfgIdHistory();
        return tableInfo;
    }
}
