package com.jjg.game.table.loongtigerwar.gamephase;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.TablePlayerGameData;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.PlayerSettleInfo;
import com.jjg.game.table.loongtigerwar.manager.LoongTigerWarSampleManager;
import com.jjg.game.table.loongtigerwar.message.resp.NotifyLoongTigerWarSettleInfo;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;
import com.jjg.game.table.loongtigerwar.room.manager.LoongTigerWarRoomGameController;
import com.jjg.game.table.redblackwar.sample.bean.WinPosWeightCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 进入结算阶段
 *
 * @author 2CL
 */
public class LoongTigerWarSettlementPhase extends BaseSettlementPhase<LoongTigerWarGameDataVo> {

    private final LoongTigerWarSampleManager loongTigerWarSampleManager;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public LoongTigerWarSettlementPhase(LoongTigerWarRoomGameController gameController) {
        super(gameController);
        loongTigerWarSampleManager = CommonUtil.getContext().getBean(LoongTigerWarSampleManager.class);
    }


    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        gameDataVo.setPhaseEndTime(getPhaseRunTime());
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
        Map<Long, Long> playerGet = new HashMap<>();
        //获取押注区域
        List<WinPosWeightCfg> weightCfgs = cfgMap.get(next);
        Map<Integer, Map<Long, Long>> betInfo = gameDataVo.getBetInfo();
        for (WinPosWeightCfg weightCfg : weightCfgs) {
            for (Integer areaId : weightCfg.getBetArea()) {
                Map<Long, Long> playerBetInfo = betInfo.get(areaId);
                if (Objects.isNull(playerBetInfo)) {
                    continue;
                }
                for (Map.Entry<Long, Long> entry : playerBetInfo.entrySet()) {
                    //计算
                    Long playerId = entry.getKey();
                    GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
                    if (gamePlayer == null) {
                        continue;
                    }
                    //返还押分
                    long backBet = entry.getValue() * weightCfg.getReturnRate() / 10000;
                    //总获得
                    long canGet = backBet * weightCfg.getOdds() / 100;
                    //TODO 抽税
                    if (weightCfg.getWinType() == 1) {
                        canGet = canGet * gameDataVo.getRoomCfg().getEffectiveRatio() / 10000;
                    }
                    canGet += backBet;
                    gamePlayer.setGold(canGet + gamePlayer.getGold());
                    playerGet.merge(playerId, canGet, Long::sum);
                }
            }
        }
        Pair<Integer, Integer> twoSpecificCard = PokerCardUtils.getTwoSpecificCard(next);
        NotifyLoongTigerWarSettleInfo warSettleInfo = new NotifyLoongTigerWarSettleInfo();
        warSettleInfo.loongCard = twoSpecificCard.getFirst();
        warSettleInfo.tigerCard = twoSpecificCard.getSecond();
        warSettleInfo.playerSettleInfos = TableMessageBuilder.getPlayerSettleInfos(playerGet);
        warSettleInfo.winState = next;
        //更新房间记录
        updateGameHistory(next);
        //清除押注历史
        betInfo.clear();
        //更新结算信息
        gameDataVo.setCurrentSettleInfo(warSettleInfo);
        //更新记录
        for (GamePlayer gamePlayer : gameDataVo.getGamePlayerMap().values()) {
            TablePlayerGameData tableGameData = gamePlayer.getTableGameData();
            long getGold = playerGet.getOrDefault(gamePlayer.getId(), 0L);
            tableGameData.addBetRecord(getGold);
        }
        //发送通知
        gameController.sendMessage(RoomMessageBuilder.newBuilder().setData(warSettleInfo));
    }

    @Override
    public void phaseFinish() {
        gameDataVo.setCurrentSettleInfo(null);
    }


    private void updateGameHistory(int result) {
        gameDataVo.addHistory(result);
    }


}
