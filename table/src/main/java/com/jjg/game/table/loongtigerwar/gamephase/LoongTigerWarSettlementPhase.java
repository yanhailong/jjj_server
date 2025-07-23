package com.jjg.game.table.loongtigerwar.gamephase;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.TablePlayerGameData;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.loongtigerwar.manager.LoongTigerWarSampleManager;
import com.jjg.game.table.loongtigerwar.message.bean.LoongTigerWarPlayerSettleInfo;
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
        gameDataVo.setPhaseEndTime(getPhaseRunTime());
        Map<Integer, List<WinPosWeightCfg>> cfgMap = loongTigerWarSampleManager.getCfgMap();
        WeightRandom<Integer> random = new WeightRandom<>();
        for (Map.Entry<Integer, List<WinPosWeightCfg>> entry : cfgMap.entrySet()) {
            //计算权重
            int total = 0;
            for (WinPosWeightCfg posWeightCfg : entry.getValue()) {
                total += posWeightCfg.getPosWeight();
            }
            random.add(total, entry.getKey());
        }
        //随机
        Integer next = random.next();
        //生成牌
        //在线玩家总获得
        long onlineTotal = 0;
        //玩家获得
        Map<Long, Long> playerGet = new HashMap<>();
        //前6玩家id
        List<Long> firstSix = gameDataVo.getFixPlayers();
        //获取押注区域
        List<WinPosWeightCfg> weightCfgs = cfgMap.get(next);
        Map<Integer, Map<Long, Long>> betInfo = gameDataVo.getBetInfo();
        for (WinPosWeightCfg weightCfg : weightCfgs) {
            for (Integer areaId : weightCfg.getBetArea()) {
                Map<Long, Long> playerBetInfo = betInfo.get(areaId);
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
                    if (!firstSix.contains(playerId)) {
                        onlineTotal += canGet;
                    }
                }
            }
        }
        Pair<Integer, Integer> twoSpecificCard = PokerCardUtils.getTwoSpecificCard(next);
        NotifyLoongTigerWarSettleInfo.Builder builder = new NotifyLoongTigerWarSettleInfo.Builder();
        builder.loongCard(twoSpecificCard.getFirst())
                .tigerCard(twoSpecificCard.getSecond())
                .playerSettleInfos(getPlayerSettleInfos(firstSix, playerGet, onlineTotal))
                .winState(next);
        //更新房间记录
        updateGameHistory(next);
        //清除押注历史
        betInfo.clear();
        //更新结算信息
        gameDataVo.setCurrentSettleInfo(builder.build());
        //发送通知
        for (GamePlayer gamePlayer : gameDataVo.getGamePlayerMap().values()) {
            TablePlayerGameData tableGameData = gamePlayer.getTableGameData();
            long getGold = playerGet.getOrDefault(gamePlayer.getId(), 0L);
            builder.getGold(getGold);
            //更新统计信息
            tableGameData.addBetRecord(getGold);
            gameController.sendMessage(gamePlayer.getId(), builder.build());
        }
    }

    @Override
    public void phaseFinish() {
        gameDataVo.setCurrentSettleInfo(null);
    }

    /**
     * 获取玩家的结算信息
     *
     * @param firstSix    前6玩家的id
     * @param playerGet   结算的玩家获得的金币
     * @param onlineTotal 在线玩家总获得
     */
    private List<LoongTigerWarPlayerSettleInfo> getPlayerSettleInfos(List<Long> firstSix, Map<Long, Long> playerGet, long onlineTotal) {
        List<LoongTigerWarPlayerSettleInfo> playerSettleInfos = new ArrayList<>();
        //前6玩家的
        for (int i = 0; i < firstSix.size(); i++) {
            Long playerId = firstSix.get(i);
            Long get = playerGet.get(playerId);
            if (Objects.nonNull(get)) {
                LoongTigerWarPlayerSettleInfo info = new LoongTigerWarPlayerSettleInfo();
                info.amount = get;
                info.playerId = playerId;
                info.index = i;
                playerSettleInfos.add(info);
            }
        }
        //在线玩家的
        LoongTigerWarPlayerSettleInfo info = new LoongTigerWarPlayerSettleInfo();
        info.amount = onlineTotal;
        info.index = -1;
        playerSettleInfos.add(info);
        return playerSettleInfos;
    }

    private void updateGameHistory(int result) {
        gameDataVo.addHistory(result);
    }


}
