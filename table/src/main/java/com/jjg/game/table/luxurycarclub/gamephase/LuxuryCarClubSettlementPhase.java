package com.jjg.game.table.luxurycarclub.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.common.utils.BetDataTrackLogUtils;
import com.jjg.game.table.luxurycarclub.data.LuxuryCarClubGameDataVo;
import com.jjg.game.table.luxurycarclub.message.LuxuryCarClubMessageBuilder;
import com.jjg.game.table.luxurycarclub.message.NotifyLuxuryCarClubSettlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        WinPosWeightCfg winPosWeightCfg = null;
        Pair<Long, Long> currentPool = canTriggerRecycling();
        if (currentPool != null) {
            WinPosWeightCfg result = generateRecyclingResults();
            if (result == null) {
                log.error("豪车俱乐部回收触发 生成结果失败 当前池:{} 标准池:{}", currentPool.getFirst(), currentPool.getSecond());
            } else {
                winPosWeightCfg = result;
                log.info("豪车俱乐部回收触发 生成结果成功 当前池:{} 标准池:{}", currentPool.getFirst(), currentPool.getSecond());
            }
        }
        if (winPosWeightCfg == null) {
            winPosWeightCfg = randomRewardCfgByWeight();
        }
        // 获取区域所有的权重值
        int clientShowPosId = winPosWeightCfg.getWinPosID();
        // 对应的下注区域ID
        Integer betAreaId = winPosWeightCfg.getBetArea().getFirst();
        // 添加中奖记录
        gameDataVo.addWinAreaCfgIdHistory(clientShowPosId);
        gameDataTracker.addGameLogData(DataTrackNameConstant.SETTLEMENT_DATA, clientShowPosId);
        NotifyLuxuryCarClubSettlement settlement =
                LuxuryCarClubMessageBuilder.notifyLuxuryCarClubSettlement(
                        (BaseTableGameController<LuxuryCarClubGameDataVo>) gameController, clientShowPosId);
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>();
        // 庄家变化的钱
        RoomBankerChangeParam changeParam = getRoomBankerChangeParam(gameDataVo.getRealPlayerAreaBetInfo());
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
                SettlementData settlementData = new SettlementData();
                BetDataTrackLogUtils.recordBetLog(settlementData, gamePlayer, gameController, playerBetInfo);
                settlementDataMap.put(playerId, settlementData);
                continue;
            }
            List<Integer> playerBetArea = playerBetInfo.get(betAreaId);
            // 玩家总押注
            long playerBetTotal = playerBetArea.stream().mapToInt(Integer::intValue).sum();
            // 给玩家进行结算
            SettlementData playerSettlementData = calcGold(gamePlayer, winPosWeightCfg, playerBetTotal);
            PlayerChangedGold playerChangedGold = new PlayerChangedGold();
            playerChangedGold.playerId = playerId;
            playerChangedGold.playerWinGold = playerSettlementData.getTotalWin();
            // 添加记录
            entry.getValue().getTableGameData().addBetRecord(playerSettlementData.getTotalWin());
            // 添加日志埋点数据
            BetDataTrackLogUtils.recordBetLog(playerSettlementData, gamePlayer, gameController, playerBetInfo);
            long totalWin = playerSettlementData.getTotalWin();
            if (totalWin > 0) {
                int addCode = gameController.addItem(gamePlayer.getId(), totalWin, AddType.GAME_SETTLEMENT, gameDataVo.getRoomCfg().getId() + "");
                if (addCode != Code.SUCCESS) {
                    log.error("豪车俱乐部结算给玩家加金币失败 playerId:{} totalWin:{} code:{}",
                            gamePlayer.getId(), totalWin, addCode);
                }
            }
            playerChangedGold.playerCurGold = gameController.getTransactionItemNum(gamePlayer.getId());
            playerChangedGolds.add(playerChangedGold);
            if (changeParam != null && !(gamePlayer instanceof GameRobotPlayer)) {
                changeParam.removeArea(betAreaId);
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
        settlement.settlementInfo.playerChangedGolds = playerChangedGolds;
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            addPlayerAreaDataLog(entry.getValue());
            // 给玩家发送结算数据
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().setData(settlement).addPlayerId(playerId));
        }
        log.debug("豪车俱乐部：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setAnimalsSettlementInfo(settlement.settlementInfo);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }

    private WinPosWeightCfg generateRecyclingResults() {
        Map<Long, Map<Integer, List<Integer>>> realPlayerBetInfo = gameDataVo.getRealPlayerBetInfo();
        if (realPlayerBetInfo == null) {
            return null;
        }
        // 获取区域所有的权重值
        Map<Integer, WinPosWeightCfg> winPosWeightCfgMap = GameDataManager.getWinPosWeightCfgMap();
        List<WinPosWeightCfg> winPosWeightCfgList = winPosWeightCfgMap.values().stream().filter(w -> w.getGameID() == EGameType.LUXURY_CAR_CLUB.getGameTypeId()).toList();
        List<WinPosWeightCfg> keys = new ArrayList<>(winPosWeightCfgList);
        Collections.shuffle(keys);
        for (WinPosWeightCfg winPosWeightCfg : keys) {
            long totalWin = 0;
            long totalLose = 0;
            for (Map.Entry<Long, Map<Integer, List<Integer>>> mapEntry : realPlayerBetInfo.entrySet()) {
                SettlementData settlementData = new SettlementData();
                List<Integer> betAreas = winPosWeightCfg.getBetArea();
                if (betAreas == null || betAreas.isEmpty()) {
                    continue;
                }
                for (Integer betAreaId : betAreas) {
                    Map<Integer, List<Integer>> playerBetInfo = mapEntry.getValue();
                    if (!playerBetInfo.containsKey(betAreaId)) {
                        continue;
                    }
                    List<Integer> playerBetGoldList = playerBetInfo.get(betAreaId);
                    // 玩家总押注
                    long playerBetGoldTotal = playerBetGoldList.stream().mapToInt(Integer::intValue).sum();
                    SettlementData calcGold = calcGold(null, winPosWeightCfg.getOdds(), winPosWeightCfg.getReturnRate(), winPosWeightCfg, playerBetGoldTotal);
                    settlementData.increaseBySettlementData(calcGold);
                }
                settlementData.setBetTotal(mapEntry.getValue().values().stream()
                        .mapToLong(a -> a.stream().mapToInt(b -> b).sum())
                        .sum());
                totalLose += settlementData.getTotalWin() + settlementData.getTaxation();
                BigDecimal totalGet = BigDecimal.valueOf(settlementData.getBetTotal() - settlementData.getBankerWind())
                        .multiply(BigDecimal.valueOf((10000 - gameDataVo.getRoomCfg().getWinRatio())))
                        .divide(BigDecimal.valueOf(10000), 4, RoundingMode.DOWN);
                totalWin += settlementData.getBankerWind() + totalGet.longValue();
            }
            if (totalWin > 0 && totalWin >= totalLose) {
                return winPosWeightCfg;
            }

        }
        return null;
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
