package com.jjg.game.table.animals.message;

import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.table.animals.AnimalsGameController;
import com.jjg.game.table.animals.data.AnimalsGameDataVo;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 飞禽走兽
 *
 * @author 2CL
 */
public class AnimalsMessageBuilder {

    private static List<Integer> CRAWL_ANIMALS_ID_LIST = Arrays.asList(5, 6, 11, 12);

    /**
     * 构建结算信息体
     */
    private static AnimalsSettlementInfo buildAnimalsSettlementInfo(
        AnimalsGameDataVo gameDataVo, AnimalsHistoryBean animalsHistoryBean) {
        AnimalsSettlementInfo animalsSettlementInfo = new AnimalsSettlementInfo();
        animalsSettlementInfo.betTableInfos = TableMessageBuilder.buildBetTableInfos(gameDataVo, false);
        animalsSettlementInfo.tableCountDownTime = gameDataVo.getPhaseEndTime();
        animalsSettlementInfo.rewardAreaIdx = animalsHistoryBean.betIdxId;
        animalsSettlementInfo.animalsId = new ArrayList<>();
        animalsSettlementInfo.animalsId.add(animalsHistoryBean.animalId);
        if (CRAWL_ANIMALS_ID_LIST.contains(animalsHistoryBean.animalId)) {
            // 走兽
            animalsSettlementInfo.animalsId.add(4);
        } else {
            // 飞禽
            animalsSettlementInfo.animalsId.add(3);
        }
        return animalsSettlementInfo;
    }

    /**
     * 结算信息
     */
    public static NotifyAnimalsSettlement notifyAnimalsSettlement(
        AnimalsGameController gameController, AnimalsHistoryBean animalsHistoryBean) {
        AnimalsGameDataVo gameDataVo = gameController.getGameDataVo();
        NotifyAnimalsSettlement settlement = new NotifyAnimalsSettlement();
        settlement.settlementInfo = buildAnimalsSettlementInfo(gameDataVo, animalsHistoryBean);
        return settlement;
    }

    /**
     * 桌面信息
     */
    public static NotifyAnimalsTableInfo notifyAnimalsTableInfo(
        AnimalsGameController gameController, boolean isInitial, long playerId) {
        NotifyAnimalsTableInfo tableInfo = new NotifyAnimalsTableInfo();
        AnimalsGameDataVo gameDataVo = gameController.getGameDataVo();
        tableInfo.gamePhase = gameController.getCurrentGamePhase();
        tableInfo.tableCountDownTime = gameDataVo.getPhaseEndTime();
        List<TablePlayerInfo> playerInfo =
            TableMessageBuilder.buildTablePlayerInfo(Collections.singletonList(playerId), gameDataVo);
        tableInfo.playerInfo = playerInfo.get(0);
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
