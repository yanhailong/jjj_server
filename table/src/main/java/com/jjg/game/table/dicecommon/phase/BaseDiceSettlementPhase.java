package com.jjg.game.table.dicecommon.phase;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.dicecommon.DiceDataHolder;
import com.jjg.game.table.dicecommon.message.BaseDiceSettlementInfo;

import java.util.*;

/**
 * 基础骰子结算阶段
 *
 * @author 2CL
 */
public abstract class BaseDiceSettlementPhase<T extends TableGameDataVo> extends BaseSettlementPhase<T> {

    public BaseDiceSettlementPhase(AbstractPhaseGameController<Room_BetCfg, T> gameController) {
        super(gameController);
    }

    public List<Integer> generateRecyclingResults(int diceNum, int diceMinNum, int diceMaxNum, EGameType gameType) {
        Map<Long, Map<Integer, List<Integer>>> realPlayerBetInfo = gameDataVo.getRealPlayerBetInfo();
        if (CollectionUtil.isEmpty(realPlayerBetInfo)) {
            return null;
        }
        List<List<Integer>> diceAllResults = getDiceAllResults(diceNum, diceMinNum, diceMaxNum);
        Collections.shuffle(diceAllResults);
        for (List<Integer> randomNumDice : diceAllResults) {
            //计算最后一位
            List<WinPosWeightCfg> winPosWeightCfgList = DiceDataHolder.getWinPosWeightCfg(gameType, randomNumDice);
            Pair<Long, Long> result = getWinOrLoseResult(realPlayerBetInfo, winPosWeightCfgList);
            if (result.getFirst() > 0 && result.getFirst() >= result.getSecond()) {
                return randomNumDice;
            }
        }
        return null;
    }

    private List<List<Integer>> getDiceAllResults(int diceNum, int diceMinNum, int diceMaxNum) {
        // 使用 Set 来去重，确保 125, 152, 512 等只保留一个排序后的结果
        Set<List<Integer>> resultSet = new HashSet<>();
        int diceRange = diceMaxNum - diceMinNum + 1;
        int times = (int) Math.pow(diceRange, diceNum);

        for (int i = 1; i <= times; i++) {
            List<Integer> randomNumDice = new ArrayList<>(diceNum);
            for (int j = 1; j <= diceNum; j++) {
                int pow = (int) Math.pow(diceRange, j);
                int number = i % pow;
                number = number == 0 ? pow : number;
                // 计算当前骰子点数
                int diceValue = (int) Math.ceil(number / (Math.pow(diceRange, j - 1))) + (diceMinNum - 1);
                randomNumDice.add(diceValue);
            }
            // 核心逻辑：排序后放入 Set 达到去重效果
            // 这样 152, 512, 215 都会变成 125 并去重
            Collections.sort(randomNumDice);
            resultSet.add(randomNumDice);
        }
        return new ArrayList<>(resultSet);
    }

    /**
     * 骰子结算
     */
    protected <S extends AbstractMessage> void settlementDice(
            BaseDiceSettlementInfo diceSettlementInfo, List<WinPosWeightCfg> winPosWeightCfgs, S settlement) {
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>();
        // 庄家变化的钱
        RoomBankerChangeParam changeParam = getRoomBankerChangeParam(gameDataVo.getRealPlayerAreaBetInfo());
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            GamePlayer gamePlayer = entry.getValue();
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
            // 玩家未下注
            if (playerBetInfo == null || playerBetInfo.isEmpty()) {
                continue;
            }
            // 给玩家进行结算
            SettlementData playerSettlementData = calcSettlementGold(gamePlayer, winPosWeightCfgs, playerBetInfo, changeParam);
            PlayerChangedGold playerChangedGold = new PlayerChangedGold();
            playerChangedGold.playerId = playerId;
            playerChangedGold.playerWinGold = playerSettlementData.getTotalWin();
            playerChangedGolds.add(playerChangedGold);
            // 给玩家添加金币
            gameController.addItem(gamePlayer.getId(), playerSettlementData.getTotalWin(), AddType.GAME_SETTLEMENT, gameDataVo.getRoomCfg().getId() + "");
            playerChangedGold.playerCurGold = gameController.getTransactionItemNum(gamePlayer.getId());
            // 添加记录
            entry.getValue().getTableGameData().addBetRecord(playerSettlementData.getTotalWin());
            if (changeParam != null && !(gamePlayer instanceof GameRobotPlayer)) {
                changeParam.addBankerChangeGold(Math.max(0, playerSettlementData.getTotalGet() - playerSettlementData.getBankerWind()));
                changeParam.addTotalTaxRevenue(playerSettlementData.getTaxation());
            }
            settlementDataMap.put(playerId, playerSettlementData);
        }
        if (changeParam != null) {
            calculationFinalBankerChange(changeParam);
            dealRoomPool(changeParam);
            gameController.dealBankerFlowing(changeParam, settlementDataMap);
        }
        // 场上玩家金币变化
        diceSettlementInfo.playerChangedGolds = playerChangedGolds;
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            addPlayerAreaDataLog(entry.getValue());
            // 给玩家发送数据
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().setData(settlement).addPlayerId(playerId));
        }
    }


}
