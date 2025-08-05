package com.jjg.game.table.dicecommon.phase;

import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
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

    public BaseDiceSettlementPhase(AbstractGameController<Room_BetCfg, T> gameController) {
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
            long playerWin = calcSettlementGold(gamePlayer, winPosWeightCfgs, playerBetInfo);
            PlayerChangedGold playerChangedGold = new PlayerChangedGold();
            playerChangedGold.playerId = playerId;
            playerChangedGold.playerWinGold = playerWin;
            playerChangedGolds.add(playerChangedGold);
            // TODO 给玩家加金币
            gamePlayer.setGold(gamePlayer.getGold() + playerWin);
            playerChangedGold.playerCurGold = gamePlayer.getGold();
            // 添加记录
            entry.getValue().getTableGameData().addBetRecord(playerWin);
        }
        // 场上玩家金币变化
        diceSettlementInfo.playerChangedGolds = playerChangedGolds;
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            diceSettlementInfo.betTableInfos =
                TableMessageBuilder.buildPlayerBetInfo(diceSettlementInfo.betTableInfos, gameDataVo, playerId);
            // 给玩家发送数据
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().setData(settlement).addPlayerId(playerId));
        }
    }

    /**
     * 计算结算金币
     */
    protected long calcSettlementGold(
        GamePlayer gamePlayer, List<WinPosWeightCfg> winPosWeightCfgs, Map<Integer, List<Integer>> playerBetInfo) {
        long playerWin = 0;
        for (WinPosWeightCfg winPosWeightCfg : winPosWeightCfgs) {
            List<Integer> betAreas = winPosWeightCfg.getBetArea();
            for (Integer betAreaIdx : betAreas) {
                if (playerBetInfo.containsKey(betAreaIdx)) {
                    List<Integer> playerBetGoldList = playerBetInfo.get(betAreaIdx);
                    // 玩家总押注
                    long playerBetGoldTotal = playerBetGoldList.stream().mapToInt(Integer::intValue).sum();
                    playerWin += calcGold(gamePlayer, winPosWeightCfg, playerBetGoldTotal);
                }
            }
        }
        return playerWin;
    }
}
