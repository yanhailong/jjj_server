package com.jjg.game.table.luxurycarclub.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.luxurycarclub.LuxuryCarClubGameController;
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

    public LuxuryCarClubSettlementPhase(AbstractGameController<Room_BetCfg, LuxuryCarClubGameDataVo> gameController) {
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
        NotifyLuxuryCarClubSettlement settlement =
            LuxuryCarClubMessageBuilder.notifyLuxuryCarClubSettlement(
                (LuxuryCarClubGameController) gameController, clientShowPosId);
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>();
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            GamePlayer gamePlayer = entry.getValue();
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
            if (playerBetInfo == null || !playerBetInfo.containsKey(betAreaId)) {
                continue;
            }
            List<Integer> playerBetArea = playerBetInfo.get(betAreaId);
            // 玩家总押注
            long playerBetTotal = playerBetArea.stream().mapToInt(Integer::intValue).sum();
            // 给玩家进行结算
            long playerWin = calcGold(winPosWeightCfg, playerBetTotal);
            PlayerChangedGold playerChangedGold = new PlayerChangedGold();
            playerChangedGold.playerId = playerId;
            playerChangedGold.playerWinGold = playerWin;
            // 添加记录
            entry.getValue().getTableGameData().addBetRecord(playerWin);
            // TODO 给玩家加金币
            gamePlayer.setGold(gamePlayer.getGold() + playerWin);
            playerChangedGold.playerCurGold = gamePlayer.getGold();
            playerChangedGolds.add(playerChangedGold);
        }
        // 场上玩家金币变化
        settlement.settlementInfo.playerChangedGolds = playerChangedGolds;
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            settlement.settlementInfo.betTableInfos =
                TableMessageBuilder.buildPlayerBetInfo(settlement.settlementInfo.betTableInfos, gameDataVo, playerId);
            // 给玩家发送结算数据
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().setData(settlement).addPlayerId(playerId));
        }
        log.debug("豪车俱乐部：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setAnimalsSettlementInfo(settlement.settlementInfo);
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
