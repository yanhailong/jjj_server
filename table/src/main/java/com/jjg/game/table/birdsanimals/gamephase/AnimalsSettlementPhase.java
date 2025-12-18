package com.jjg.game.table.birdsanimals.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.AddType;
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
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.birdsanimals.data.AnimalsGameDataVo;
import com.jjg.game.table.birdsanimals.message.AnimalsHistoryBean;
import com.jjg.game.table.birdsanimals.message.AnimalsMessageBuilder;
import com.jjg.game.table.birdsanimals.message.NotifyAnimalsSettlement;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.common.utils.BetDataTrackLogUtils;

import java.util.*;

/**
 * 飞禽走兽结算
 *
 * @author 2CL
 */
public class AnimalsSettlementPhase extends BaseSettlementPhase<AnimalsGameDataVo> {

    public AnimalsSettlementPhase(AbstractPhaseGameController<Room_BetCfg, AnimalsGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 获取区域所有的权重值
        Map<Integer, WinPosWeightCfg> winPosWeightCfgMap = GameDataManager.getWinPosWeightCfgMap();
        List<WinPosWeightCfg> winPosWeightCfgList =
                winPosWeightCfgMap.values().stream().filter(w -> w.getGameID() == EGameType.BIRDS_ANIMAL.getGameTypeId()).toList();
        // PosId对应的总权重
        Map<Integer, Integer> weightMap = new HashMap<>();
        // PosID 对应的 开奖配置列表
        Map<Integer, List<WinPosWeightCfg>> winPosOfWeightCfgs = new HashMap<>();
        // 通过winPosID进行聚合
        for (WinPosWeightCfg winPosWeightCfg : winPosWeightCfgList) {
            weightMap.put(
                    winPosWeightCfg.getWinPosID(),
                    weightMap.getOrDefault(winPosWeightCfg.getWinPosID(), 0) + winPosWeightCfg.getPosWeight());
            winPosOfWeightCfgs.computeIfAbsent(winPosWeightCfg.getWinPosID(), k -> new ArrayList<>()).add(winPosWeightCfg);
        }
        // 中奖区域，应该是一个动物和对应他的分类
        List<WinPosWeightCfg> winPosWeightCfgs = null;
        int randomRewardPosId = 0;
        long currentPool = canTriggerRecycling();
        if (currentPool > 0) {
            Pair<Integer, List<WinPosWeightCfg>> result = generateRecyclingResults(winPosOfWeightCfgs);
            if (result == null) {
                log.error("飞禽走兽回收触发 生成结果失败 当前池:{} 标准池:{}", currentPool, gameDataVo.getRoomCfg().getInitBasePool());
            } else {
                winPosWeightCfgs = result.getSecond();
                randomRewardPosId = result.getFirst();
                log.info("飞禽走兽回收触发 生成结果成功 当前池:{} 标准池:{}", currentPool, gameDataVo.getRoomCfg().getInitBasePool());
            }
        }
        if (winPosWeightCfgs == null) {
            Set<Integer> winPosIds = RandomUtils.getRandomByWeight(weightMap, 1);
            randomRewardPosId = winPosIds.iterator().next();
            winPosWeightCfgs = winPosOfWeightCfgs.get(randomRewardPosId);
        }
        // 添加中奖记录
        AnimalsHistoryBean historyBean = addAnimalsHistory(randomRewardPosId, winPosWeightCfgs);
        NotifyAnimalsSettlement settlement =
                AnimalsMessageBuilder.notifyAnimalsSettlement(
                        (BaseTableGameController<AnimalsGameDataVo>) gameController, historyBean);
        gameDataTracker.addGameLogData(DataTrackNameConstant.SETTLEMENT_DATA, historyBean);
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>();
        // 庄家变化的钱
        RoomBankerChangeParam changeParam = getRoomBankerChangeParam(gameDataVo.getBetInfo());
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            GamePlayer gamePlayer = entry.getValue();
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
            // 玩家未下注
            if (playerBetInfo == null || playerBetInfo.isEmpty()) {
                // 添加记录
                continue;
            }
            // 给玩家进行结算
            SettlementData settlementData = calcSettlementGold(gamePlayer, winPosWeightCfgs, playerBetInfo, changeParam);
            PlayerChangedGold playerChangedGold = new PlayerChangedGold();
            playerChangedGold.playerId = playerId;
            playerChangedGold.playerWinGold = settlementData.getBetWin();
            // 添加记录
            entry.getValue().getTableGameData().addBetRecord(settlementData.getTotalWin());
            // 给玩家添加金币
            gameController.addItem(gamePlayer.getId(), settlementData.getTotalWin(), AddType.GAME_SETTLEMENT, gameDataVo.getRoomCfg().getId() + "");
            playerChangedGold.playerCurGold = gameController.getTransactionItemNum(gamePlayer.getId());
            playerChangedGolds.add(playerChangedGold);
            settlementDataMap.put(playerId, settlementData);
        }
        if (changeParam != null) {
            calculationFinalBankerChange(changeParam);
            gameController.dealBankerFlowing(changeParam, settlementDataMap);
        }
        dealRoomPool(settlementDataMap);
        // 场上玩家金币变化
        settlement.settlementInfo.playerChangedGolds = playerChangedGolds;
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            settlement.settlementInfo.betTableInfos = TableMessageBuilder.buildPlayerBetInfo(settlement.settlementInfo.betTableInfos, gameDataVo, playerId);
            // 给玩家发送结算数据
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().setData(settlement).addPlayerId(playerId));
            if (gameDataVo.getPlayerBetInfo().containsKey(playerId)) {
                gameDataTracker.addPlayerLogData(entry.getValue(), DataTrackNameConstant.AREA_DATA, JSON.toJSONString(settlement.settlementInfo.betTableInfos));
            }
        }
        log.debug("飞禽走兽房间：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setAnimalsSettlementInfo(settlement.settlementInfo);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }

    private Pair<Integer, List<WinPosWeightCfg>> generateRecyclingResults(Map<Integer, List<WinPosWeightCfg>> winPosOfWeightCfgMap) {
        Map<Long, Map<Integer, List<Integer>>> realPlayerBetInfo = gameDataVo.getRealPlayerBetInfo();
        if (realPlayerBetInfo == null) {
            return null;
        }
        // 2. 提前过滤：移除包含大奖（WinType 1或2）的无效 Key，减少后续 shuffle 和循环的负担
        List<Integer> validKeys = getValidKeys(winPosOfWeightCfgMap);
        if (validKeys.isEmpty()) {
            return null;
        }
        Collections.shuffle(validKeys);
        for (Integer key : validKeys) {
            List<WinPosWeightCfg> winPosWeightCfgList = winPosOfWeightCfgMap.get(key);
            Pair<Long, Long> winOrLoseResult = getWinOrLoseResult(realPlayerBetInfo, winPosWeightCfgList);
            if (winOrLoseResult.getFirst() > 0 && winOrLoseResult.getFirst() >= winOrLoseResult.getSecond()) {
                return Pair.newPair(key, winPosWeightCfgList);
            }
        }
        return null;
    }

    private List<Integer> getValidKeys(Map<Integer, List<WinPosWeightCfg>> winPosOfWeightCfgMap) {
        List<Integer> validKeys = new ArrayList<>();
        for (Map.Entry<Integer, List<WinPosWeightCfg>> entry : winPosOfWeightCfgMap.entrySet()) {
            boolean hasBigWin = false;
            for (WinPosWeightCfg cfg : entry.getValue()) {
                if (cfg.getWinType() == 1 || cfg.getWinType() == 2) {
                    hasBigWin = true;
                    break;
                }
            }
            if (!hasBigWin) {
                validKeys.add(entry.getKey());
            }
        }
        return validKeys;
    }

    /**
     * 添加飞禽走兽的中奖历史记录
     */
    private AnimalsHistoryBean addAnimalsHistory(int randomRewardPosId, List<WinPosWeightCfg> winPosWeightCfgs) {
        AnimalsHistoryBean animalsHistoryBean = new AnimalsHistoryBean();
        animalsHistoryBean.betIdxId = randomRewardPosId;
        for (WinPosWeightCfg winPosWeightCfg : winPosWeightCfgs) {
            if (winPosWeightCfg.getWinPosID() == 13) {
                // 通赔
                animalsHistoryBean.animalId = 14;
            } else if (winPosWeightCfg.getWinPosID() == 27) {
                // 通杀
                animalsHistoryBean.animalId = 13;
            } else {
                int betArea = winPosWeightCfg.getBetArea().get(0);
                int gameTypeId = EGameType.BIRDS_ANIMAL.getGameTypeId();
                int crawlArea = gameTypeId * 100 + 3, flyArea = gameTypeId * 100 + 4;
                // 飞禽 走兽区域
                if (betArea != crawlArea && betArea != flyArea) {
                    // 中奖区域的ID后两位和前端的一致对应
                    animalsHistoryBean.animalId = betArea % 100;
                }
            }
            if (animalsHistoryBean.animalId != 0) {
                break;
            }
        }
        // 添加记录
        gameDataVo.addWinAreaCfgIdHistory(animalsHistoryBean);
        return animalsHistoryBean;
    }

    /**
     * 结算金币
     */
    @Override
    public SettlementData calcSettlementGold(GamePlayer gamePlayer, List<WinPosWeightCfg> winPosWeightCfgs,
                                             Map<Integer, List<Integer>> playerBetInfo, RoomBankerChangeParam changeParam) {
        SettlementData settlementData = new SettlementData();
        for (WinPosWeightCfg winPosWeightCfg : winPosWeightCfgs) {
            if (winPosWeightCfg.getWinType() == 1) {
                //通杀
                continue;
            }
            List<Integer> betAreas = winPosWeightCfg.getBetArea();
            if (betAreas == null || betAreas.isEmpty()) {
                log.error("配置表异常：winPosWeight表中的飞禽走兽ID：{} 的配置没有配置betArea", winPosWeightCfg.getId());
                continue;
            }
            for (Integer betAreaId : betAreas) {
                if (!playerBetInfo.containsKey(betAreaId)) {
                    continue;
                }
                List<Integer> playerBetGoldList = playerBetInfo.get(betAreaId);
                // 玩家总押注
                long playerBetGoldTotal = playerBetGoldList.stream().mapToInt(Integer::intValue).sum();
                int odds = 0;
                int returnRate = 0;
                if (winPosWeightCfg.getWinType() == 2) {
                    //通赔
                    BetAreaCfg betAreaCfg = GameDataManager.getBetAreaCfg(betAreaId);
                    if (betAreaCfg != null) {
                        odds = betAreaCfg.getBaseBet();
                        returnRate = betAreaCfg.getBaseReturnRate();
                    }
                } else {
                    odds = winPosWeightCfg.getOdds();
                    returnRate = winPosWeightCfg.getReturnRate();
                }
                SettlementData calcGold = calcGold(gamePlayer, odds, returnRate, winPosWeightCfg, playerBetGoldTotal);
                settlementData.increaseBySettlementData(calcGold);
                if (changeParam != null) {
                    changeParam.removeArea(betAreaId);
                }
            }
        }
        if (changeParam != null) {
            changeParam.addTotalTaxRevenue(settlementData.getTaxation());
            changeParam.addBankerChangeGold(Math.max(0, settlementData.getTotalWin() - settlementData.getBetTotal()));
        }
        if (!(gamePlayer instanceof GameRobotPlayer)) {
            // 总押注
            BetDataTrackLogUtils.recordBetLog(settlementData, gamePlayer, gameController, playerBetInfo);
        }
        return settlementData;
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
