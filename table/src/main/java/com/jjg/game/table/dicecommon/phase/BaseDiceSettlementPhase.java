package com.jjg.game.table.dicecommon.phase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.common.utils.BetDataTrackLogUtils;
import com.jjg.game.table.dicecommon.message.BaseDiceSettlementInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基础骰子结算阶段
 *
 * @author 2CL
 */
public abstract class BaseDiceSettlementPhase<T extends TableGameDataVo> extends BaseSettlementPhase<T> {

    public BaseDiceSettlementPhase(AbstractPhaseGameController<Room_BetCfg, T> gameController) {
        super(gameController);
    }

    /**
     * 骰子结算
     */
    protected <S extends AbstractMessage> void settlementDice(
        BaseDiceSettlementInfo diceSettlementInfo, List<WinPosWeightCfg> winPosWeightCfgs, S settlement) {
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>();
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            GamePlayer gamePlayer = entry.getValue();
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
            // 玩家未下注
            if (playerBetInfo == null || playerBetInfo.isEmpty()) {
                continue;
            }
            // 给玩家进行结算
            SettlementData playerSettlementData = calcSettlementGold(gamePlayer, winPosWeightCfgs, playerBetInfo);
            PlayerChangedGold playerChangedGold = new PlayerChangedGold();
            playerChangedGold.playerId = playerId;
            playerChangedGold.playerWinGold = playerSettlementData.getBetWin();
            playerChangedGolds.add(playerChangedGold);
            // 给玩家添加金币
            gameController.addGold(
                gamePlayer.getId(), playerSettlementData.getTotalWin(),
                ERoomItemReason.GAME_SETTLEMENT.withCfgId(gameDataVo.getRoomCfg().getId()));
            playerChangedGold.playerCurGold = gamePlayer.getGold();
            // 添加记录
            entry.getValue().getTableGameData().addBetRecord(playerSettlementData.getTotalWin());
        }
        // 场上玩家金币变化
        diceSettlementInfo.playerChangedGolds = playerChangedGolds;
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            diceSettlementInfo.betTableInfos =
                TableMessageBuilder.buildPlayerBetInfo(diceSettlementInfo.betTableInfos, gameDataVo, playerId);
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
            // 玩家未下注
            if (playerBetInfo != null && !playerBetInfo.isEmpty()) {
                gameDataTracker.addPlayerLogData(
                    entry.getValue(),
                    DataTrackNameConstant.AREA_DATA,
                    JSON.toJSONString(diceSettlementInfo.betTableInfos));
            }
            // 给玩家发送数据
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().setData(settlement).addPlayerId(playerId));
        }
    }

    /**
     * 计算结算金币
     */
    protected SettlementData calcSettlementGold(
        GamePlayer gamePlayer, List<WinPosWeightCfg> winPosWeightCfgs, Map<Integer, List<Integer>> playerBetInfo) {
        SettlementData playerSettlementData = new SettlementData();
        for (WinPosWeightCfg winPosWeightCfg : winPosWeightCfgs) {
            List<Integer> betAreas = winPosWeightCfg.getBetArea();
            for (Integer betAreaIdx : betAreas) {
                if (playerBetInfo.containsKey(betAreaIdx)) {
                    List<Integer> playerBetGoldList = playerBetInfo.get(betAreaIdx);
                    // 玩家总押注
                    long playerBetGoldTotal = playerBetGoldList.stream().mapToInt(Integer::intValue).sum();
                    SettlementData settlementData = calcGold(gamePlayer, winPosWeightCfg, playerBetGoldTotal);
                    playerSettlementData.increaseBySettlementData(settlementData);
                }
            }
        }
        // 记录日志
        BetDataTrackLogUtils.recordBetLog(playerSettlementData, gamePlayer, gameDataTracker, playerBetInfo);
        return playerSettlementData;
    }
}
