package com.jjg.game.table.luxurycarclub.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.common.utils.BetDataTrackLogUtils;
import com.jjg.game.table.luxurycarclub.data.LuxuryCarClubGameDataVo;
import com.jjg.game.table.luxurycarclub.message.LuxuryCarClubMessageBuilder;
import com.jjg.game.table.luxurycarclub.message.NotifyLuxuryCarClubSettlement;

import java.util.*;

/**
 * 豪车俱乐部结算
 *
 * @author 2CL
 */
public class LuxuryCarClubSettlementPhase extends BaseSettlementPhase<LuxuryCarClubGameDataVo> {

    public LuxuryCarClubSettlementPhase(AbstractPhaseGameController<Room_BetCfg, LuxuryCarClubGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        WinPosWeightCfg winPosWeightCfg = randomRewardCfgByWeight();
        // 获取区域所有的权重值
        int clientShowPosId = winPosWeightCfg.getWinPosID();
        // 对应的下注区域ID
        Integer betAreaId = winPosWeightCfg.getBetArea().get(0);
        // 添加中奖记录
        gameDataVo.addWinAreaCfgIdHistory(clientShowPosId);
        gameDataTracker.addGameLogData(DataTrackNameConstant.SETTLEMENT_DATA, clientShowPosId);
        NotifyLuxuryCarClubSettlement settlement =
            LuxuryCarClubMessageBuilder.notifyLuxuryCarClubSettlement(
                (BaseTableGameController<LuxuryCarClubGameDataVo>) gameController, clientShowPosId);
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>();
        // 庄家变化的钱
        long bankerChangeGold = 0;
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            GamePlayer gamePlayer = entry.getValue();
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
            if (playerBetInfo == null) {
                // 添加记录
                continue;
            }
            if (!playerBetInfo.containsKey(betAreaId)) {
                BetDataTrackLogUtils.recordBetLog(new SettlementData(), gamePlayer, gameController, playerBetInfo);
                continue;
            }
            List<Integer> playerBetArea = playerBetInfo.get(betAreaId);
            // 玩家总押注
            long playerBetTotal = playerBetArea.stream().mapToInt(Integer::intValue).sum();
            // 给玩家进行结算
            SettlementData playerSettlementData = calcGold(gamePlayer, winPosWeightCfg, playerBetTotal);
            PlayerChangedGold playerChangedGold = new PlayerChangedGold();
            playerChangedGold.playerId = playerId;
            playerChangedGold.playerWinGold = playerSettlementData.getBetWin();
            // 添加记录
            entry.getValue().getTableGameData().addBetRecord(playerSettlementData.getTotalWin());
            // 添加日志埋点数据
            BetDataTrackLogUtils.recordBetLog(playerSettlementData, gamePlayer, gameController, playerBetInfo);
            // 给玩家添加金币
            gameController.addItem(
                gamePlayer.getId(), playerSettlementData.getTotalWin(),
                ERoomItemReason.GAME_SETTLEMENT.withCfgId(gameDataVo.getRoomCfg().getId()));
            playerChangedGold.playerCurGold = gameController.getItemNum(gamePlayer.getId());
            playerChangedGolds.add(playerChangedGold);
            bankerChangeGold += playerSettlementData.getTotalWin() - playerSettlementData.getBetTotal();
            settlementDataMap.put(playerId, playerSettlementData);
        }
        gameController.dealBankerFlowing(bankerChangeGold, settlementDataMap);
        // 场上玩家金币变化
        settlement.settlementInfo.playerChangedGolds = playerChangedGolds;
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            settlement.settlementInfo.betTableInfos =
                TableMessageBuilder.buildPlayerBetInfo(settlement.settlementInfo.betTableInfos, gameDataVo, playerId);
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
            if (playerBetInfo != null) {
                gameDataTracker.addPlayerLogData(
                    entry.getValue(), DataTrackNameConstant.AREA_DATA,
                    JSON.toJSONString(settlement.settlementInfo.betTableInfos));
            }
            // 给玩家发送结算数据
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().setData(settlement).addPlayerId(playerId));
        }
        log.debug("豪车俱乐部：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setAnimalsSettlementInfo(settlement.settlementInfo);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }

    /**
     * 根据权重随机一个位置ID
     */
    public static WinPosWeightCfg randomRewardCfgByWeight() {
        // 获取区域所有的权重值
        Map<Integer, WinPosWeightCfg> winPosWeightCfgMap = GameDataManager.getWinPosWeightCfgMap();
        List<WinPosWeightCfg> winPosWeightCfgList =
            winPosWeightCfgMap.values().stream().filter(w -> w.getGameID() == EGameType.LUXURY_CAR_CLUB.getGameTypeId()).toList();
        // PosId对应的总权重
        Map<Integer, Integer> weightMap = new HashMap<>();
        // 通过winPosID进行聚合
        for (WinPosWeightCfg winPosWeightCfg : winPosWeightCfgList) {
            weightMap.put(
                winPosWeightCfg.getId(),
                weightMap.getOrDefault(winPosWeightCfg.getId(), 0) + winPosWeightCfg.getPosWeight());
        }
        Set<Integer> winPosIds = RandomUtils.getRandomByWeight(weightMap, 1);
        int randomRewardPosId = winPosIds.iterator().next();
        // 中奖区域ID
        return winPosWeightCfgMap.get(randomRewardPosId);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
