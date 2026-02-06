package com.jjg.game.table.luxurycarclub.message;

import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;
import com.jjg.game.table.luxurycarclub.data.LuxuryCarClubGameDataVo;

import java.util.Collections;
import java.util.List;

/**
 * 豪车俱乐部
 *
 * @author 2CL
 */
public class LuxuryCarClubMessageBuilder {

    /**
     * 构建结算信息体
     */
    private static LuxuryCarClubSettlementInfo buildLuxuryCarClubSettlementInfo(
            LuxuryCarClubGameDataVo gameDataVo, int rewardPosId) {
        LuxuryCarClubSettlementInfo luxuryCarClubSettlementInfo = new LuxuryCarClubSettlementInfo();
        luxuryCarClubSettlementInfo.betTableInfos = TableMessageBuilder.buildBetTableInfos(gameDataVo, false);
        luxuryCarClubSettlementInfo.tableCountDownTime = gameDataVo.getPhaseEndTime();
        luxuryCarClubSettlementInfo.rewardAreaIdx = rewardPosId;
        return luxuryCarClubSettlementInfo;
    }

    /**
     * 结算信息
     */
    public static NotifyLuxuryCarClubSettlement notifyLuxuryCarClubSettlement(
            BaseTableGameController<LuxuryCarClubGameDataVo> gameController, int rewardPosId) {
        LuxuryCarClubGameDataVo gameDataVo = gameController.getGameDataVo();
        NotifyLuxuryCarClubSettlement settlement = new NotifyLuxuryCarClubSettlement();
        settlement.settlementInfo = buildLuxuryCarClubSettlementInfo(gameDataVo, rewardPosId);
        return settlement;
    }

    /**
     * 桌面信息
     */
    public static NotifyLuxuryCarClubTableInfo notifyLuxuryCarClubTableInfo(
            BaseTableGameController<LuxuryCarClubGameDataVo> gameController, boolean isInitial, long playerId) {
        NotifyLuxuryCarClubTableInfo tableInfo = new NotifyLuxuryCarClubTableInfo();
        LuxuryCarClubGameDataVo gameDataVo = gameController.getGameDataVo();
        tableInfo.gamePhase = gameController.getCurrentGamePhase();
        tableInfo.tableCountDownTime = gameDataVo.getPhaseEndTime();
        List<TablePlayerInfo> playerInfo =
                TableMessageBuilder.buildTablePlayerInfo(gameController, Collections.singletonList(playerId), gameDataVo);
        if (!playerInfo.isEmpty()) {
            tableInfo.playerInfo = playerInfo.getFirst();
        }
        if (tableInfo.gamePhase == EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            tableInfo.settlementInfo = gameDataVo.getAnimalsSettlementInfo();
        }
        if (isInitial) {
            tableInfo.betPointList = gameDataVo.getRoomCfg().getBetList();
        }
        tableInfo.totalPlayerNum = gameDataVo.getPlayerNum();
        tableInfo.settlementHistory = gameDataVo.getWinAreaCfgIdHistory();
        tableInfo.tableAreaInfos = TableMessageBuilder.buildBetTableInfos(gameDataVo, isInitial);
        tableInfo.maxChipOnTable =
                GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.MAX_CHIP_ON_TABLE).getIntValue();
        return tableInfo;
    }
}
