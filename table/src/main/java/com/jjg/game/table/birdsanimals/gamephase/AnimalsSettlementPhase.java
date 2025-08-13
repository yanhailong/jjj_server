package com.jjg.game.table.birdsanimals.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.birdsanimals.AnimalsGameController;
import com.jjg.game.table.birdsanimals.data.AnimalsGameDataVo;
import com.jjg.game.table.birdsanimals.message.AnimalsHistoryBean;
import com.jjg.game.table.birdsanimals.message.AnimalsMessageBuilder;
import com.jjg.game.table.birdsanimals.message.NotifyAnimalsSettlement;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.betsample.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;

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
        Set<Integer> winPosIds = RandomUtils.getRandomByWeight(weightMap, 1);
        int randomRewardPosId = winPosIds.iterator().next();
        // 中奖区域，应该是一个动物和对应他的分类
        List<WinPosWeightCfg> winPosWeightCfgs = winPosOfWeightCfgs.get(randomRewardPosId);
        // 添加中奖记录
        AnimalsHistoryBean historyBean = addAnimalsHistory(randomRewardPosId, winPosWeightCfgs);
        NotifyAnimalsSettlement settlement =
            AnimalsMessageBuilder.notifyAnimalsSettlement((AnimalsGameController) gameController, historyBean);
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>();
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
            SettlementData settlementData = calcSettlementGold(gamePlayer, winPosWeightCfgs, playerBetInfo);
            PlayerChangedGold playerChangedGold = new PlayerChangedGold();
            playerChangedGold.playerId = playerId;
            playerChangedGold.playerWinGold = settlementData.getBetWin();
            // 添加记录
            entry.getValue().getTableGameData().addBetRecord(settlementData.getTotalWin());
            // TODO 给玩家加金币
            gamePlayer.setGold(gamePlayer.getGold() + settlementData.getTotalWin());
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
        log.debug("飞禽走兽房间：{} 结算数据：{}", gameDataVo.getRoomCfg().getId(), JSON.toJSONString(settlement));
        // 保存记录
        gameDataVo.setAnimalsSettlementInfo(settlement.settlementInfo);
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
    private SettlementData calcSettlementGold(
        GamePlayer gamePlayer, List<WinPosWeightCfg> winPosWeightCfgs, Map<Integer, List<Integer>> playerBetInfo) {
        SettlementData settlementData = new SettlementData();
        for (WinPosWeightCfg winPosWeightCfg : winPosWeightCfgs) {
            List<Integer> betAreas = winPosWeightCfg.getBetArea();
            if (betAreas == null || betAreas.isEmpty()) {
                log.error("配置表异常：winPosWeight表中的飞禽走兽ID：{} 的配置没有配置betArea", winPosWeightCfg.getId());
            }
            int betAreaId = betAreas.get(0);
            if (winPosWeightCfg.getWinType() == 0 && playerBetInfo.containsKey(betAreaId)) {
                List<Integer> playerBetGoldList = playerBetInfo.get(betAreaId);
                // 玩家总押注
                long playerBetGoldTotal = playerBetGoldList.stream().mapToInt(Integer::intValue).sum();
                 SettlementData calcGold = calcGold(gamePlayer, winPosWeightCfg, playerBetGoldTotal);
                 settlementData.increaseBySettlementData(calcGold);
            } else if (winPosWeightCfg.getWinType() == 4) {
                // 通赔逻辑
                for (Map.Entry<Integer, List<Integer>> entry : playerBetInfo.entrySet()) {
                    BetAreaCfg betAreaCfg = GameDataManager.getBetAreaCfg(entry.getKey());
                    List<Integer> posWinList = betAreaCfg.getPosWin();
                    int posId = posWinList.get(0);
                    WinPosWeightCfg weightCfg = GameDataManager.getWinPosWeightCfg(posId);
                    int playerBetGoldTotal = entry.getValue().stream().mapToInt(Integer::intValue).sum();
                    SettlementData calcGold = calcGold(gamePlayer, weightCfg, playerBetGoldTotal);
                    settlementData.increaseBySettlementData(calcGold);
                }
            }
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
