package com.jjg.game.table.dicecommon.message;

import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.table.common.TableConstant;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.message.TableMessageBuilder;

/**
 * 骰子信息构建器
 *
 * @author 2CL
 */
public class BaseDiceMessageBuilder {

    /**
     * 构建基础骰子结算信息
     */
    public static BaseDiceSettlementInfo buildDiceSettlementInfo(TableGameDataVo tableGameDataVo) {
        BaseDiceSettlementInfo diceSettlementInfo = new BaseDiceSettlementInfo();
        diceSettlementInfo.tableCountDownTime = tableGameDataVo.getPhaseEndTime();
        diceSettlementInfo.betTableInfos = TableMessageBuilder.buildBetTableInfos(tableGameDataVo, false);
        return diceSettlementInfo;
    }

    /**
     * 构建骰子类牌桌信息
     */
    public static BaseDiceTableInfo buildDiceTableInfo(
        long playerId, EGamePhase curGamePhase, TableGameDataVo gameDataVo, boolean isInitial) {
        BaseDiceTableInfo diceTableInfo = new BaseDiceTableInfo();
        diceTableInfo.gamePhase = curGamePhase;
        diceTableInfo.tableCountDownTime = gameDataVo.getPhaseEndTime();
        diceTableInfo.playerInfo =
            TableMessageBuilder.buildTablePlayerInfo(playerId, gameDataVo, TableConstant.ON_TABLE_PLAYER_NUM);
        if (isInitial) {
            diceTableInfo.betPointList = gameDataVo.getRoomCfg().getBetList();
        }
        diceTableInfo.totalPlayerNum = gameDataVo.getPlayerNum();
        diceTableInfo.tableAreaInfos = TableMessageBuilder.buildBetTableInfos(gameDataVo, isInitial);
        diceTableInfo.maxChipOnTable =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.MAX_CHIP_ON_TABLE).getIntValue();
        return diceTableInfo;
    }
}
