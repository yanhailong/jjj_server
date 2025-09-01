package com.jjg.game.table.loongtigerwar.gamephase;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.data.room.TablePlayerGameData;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.datatrack.SaveLogUtil;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.loongtigerwar.manager.LoongTigerWarSampleManager;
import com.jjg.game.table.loongtigerwar.message.resp.NotifyLoongTigerWarSettleInfo;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 进入结算阶段
 *
 * @author 2CL
 */
public class LoongTigerWarSettlementPhase extends BaseSettlementPhase<LoongTigerWarGameDataVo> {

    private final LoongTigerWarSampleManager loongTigerWarSampleManager;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public LoongTigerWarSettlementPhase(BaseTableGameController<LoongTigerWarGameDataVo> gameController) {
        super(gameController);
        loongTigerWarSampleManager = CommonUtil.getContext().getBean(LoongTigerWarSampleManager.class);
    }


    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        Map<Integer, List<WinPosWeightCfg>> cfgMap = loongTigerWarSampleManager.getCfgMap();
        WeightRandom<Integer> random = new WeightRandom<>();
        for (Map.Entry<Integer, List<WinPosWeightCfg>> entry : cfgMap.entrySet()) {
            //计算权重
            int total = 0;
            for (WinPosWeightCfg posWeightCfg : entry.getValue()) {
                total += posWeightCfg.getPosWeight();
            }
            random.add(entry.getKey(), total);
        }
        //随机
        Integer next = random.next();
        //玩家获得
        Map<Long, DefaultKeyValue<Long, Long>> playerGet = new HashMap<>();
        //获取押注区域
        List<WinPosWeightCfg> weightCfgs = cfgMap.get(next);
        Map<Integer, Map<Long, List<Integer>>> betInfo = gameDataVo.getBetInfo();
        // 庄家变化的钱
        long bankerChangeGold = 0;
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();
        for (WinPosWeightCfg weightCfg : weightCfgs) {
            for (Integer areaId : weightCfg.getBetArea()) {
                Map<Long, List<Integer>> playerBetInfo = betInfo.get(areaId);
                if (Objects.isNull(playerBetInfo)) {
                    continue;
                }
                for (Map.Entry<Long, List<Integer>> entry : playerBetInfo.entrySet()) {
                    //计算
                    Long playerId = entry.getKey();
                    int totalBet = entry.getValue().stream().mapToInt(Integer::intValue).sum();
                    GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
                    if (gamePlayer == null) {
                        continue;
                    }
                    //返还押分
                    long backBet = (long) totalBet * weightCfg.getReturnRate() / 10000;
                    //总获得
                    long canGet = backBet * weightCfg.getOdds() / 100;
                    if (weightCfg.getIsRatio() == 1) {
                        canGet = canGet * gameDataVo.getRoomCfg().getEffectiveRatio() / 10000;
                    }
                    canGet += backBet;
                    SettlementData settlementData = new SettlementData(canGet - backBet, backBet, canGet, totalBet);
                    if (!settlementDataMap.containsKey(playerId)) {
                        settlementDataMap.put(playerId, settlementData);
                    } else {
                        settlementDataMap.get(playerId).increaseBySettlementData(settlementData);
                    }
                    bankerChangeGold +=
                        settlementDataMap.get(playerId).getTotalWin() - settlementDataMap.get(playerId).getBetTotal();
                    // 给玩家添加金币
                    gameController.addGold(
                        gamePlayer.getId(), canGet,
                        ERoomItemReason.GAME_SETTLEMENT.withCfgId(gameDataVo.getRoomCfg().getId()));
                    DefaultKeyValue<Long, Long> keyValue = playerGet.computeIfAbsent(playerId,
                        key -> new DefaultKeyValue<>(0L, 0L));
                    keyValue.setKey(keyValue.getKey() + totalBet);
                    keyValue.setValue(keyValue.getValue() + canGet);
                }
            }
        }
        gameController.dealBankerFlowing(bankerChangeGold, settlementDataMap);
        Pair<Integer, Integer> twoSpecificCard = PokerCardUtils.getTwoSpecificCard(next);
        NotifyLoongTigerWarSettleInfo warSettleInfo = new NotifyLoongTigerWarSettleInfo();
        warSettleInfo.loongCard = twoSpecificCard.getFirst();
        warSettleInfo.tigerCard = twoSpecificCard.getSecond();
        warSettleInfo.playerSettleInfos = TableMessageBuilder.getPlayerSettleInfos(playerGet, gameDataVo);
        warSettleInfo.winState = next;
        warSettleInfo.waitEndTime = gameDataVo.getPhaseEndTime();
        //更新房间记录
        updateGameHistory(next);
        addLog(gameDataVo, playerGet, warSettleInfo.loongCard, warSettleInfo.tigerCard);
        //清除押注历史
        betInfo.clear();
        //更新结算信息
        gameDataVo.setCurrentSettleInfo(warSettleInfo);
        //更新记录
        for (GamePlayer gamePlayer : gameDataVo.getGamePlayerMap().values()) {
            TablePlayerGameData tableGameData = gamePlayer.getTableGameData();
            DefaultKeyValue<Long, Long> keyValue = playerGet.get(gamePlayer.getId());
            long getGold = keyValue == null ? 0 : keyValue.getValue() - keyValue.getKey();
            tableGameData.addBetRecord(getGold);
        }
        //发送通知
        broadcastMsgToRoom(warSettleInfo);
    }

    @Override
    public void phaseFinishAction() {
        gameDataVo.setCurrentSettleInfo(null);
    }


    private void updateGameHistory(int result) {
        gameDataVo.addHistory(result);
    }

    private void addLog(LoongTigerWarGameDataVo gameDataVo, Map<Long, DefaultKeyValue<Long, Long>> playerGetInfo,
                        int loongCard, int tigerCard) {
        SaveLogUtil.generalLog(gameDataVo.getPlayerBetInfo(), playerGetInfo, gameDataVo.getGamePlayerMap(),
            gameController);
        gameDataTracker.addGameLogData("loongCard", loongCard);
        gameDataTracker.addGameLogData("tigerCard", tigerCard);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }
}
