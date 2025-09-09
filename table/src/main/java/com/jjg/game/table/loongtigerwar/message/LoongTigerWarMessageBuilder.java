package com.jjg.game.table.loongtigerwar.message;

import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetPlayerChip;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.loongtigerwar.message.resp.NotifyLoongTigerWarInfo;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 2CL
 */
public class LoongTigerWarMessageBuilder {

    /**
     * 构架初始化信息
     */
    public static NotifyLoongTigerWarInfo buildInitInfo(
        long playerId, LoongTigerWarGameDataVo dataVo, EGamePhase gamePhase) {
        NotifyLoongTigerWarInfo notifyLoongTigerWarInfo = new NotifyLoongTigerWarInfo();
        //历史记录
        notifyLoongTigerWarInfo.histories = dataVo.getHistories();
        //阶段信息
        notifyLoongTigerWarInfo.gamePhase = gamePhase;
        //阶段结束时间
        notifyLoongTigerWarInfo.tableCountDownTime = dataVo.getPhaseEndTime();
        //各区域押注信息
        Map<Integer, Map<Long, List<Integer>>> betInfoMap = dataVo.getBetInfo();
        if (!betInfoMap.isEmpty()) {
            List<BetTableInfo> tableAreaInfos = new ArrayList<>();
            //遍历押注信息
            for (Map.Entry<Integer, Map<Long, List<Integer>>> mapEntry : betInfoMap.entrySet()) {
                Map<Long, List<Integer>> playerBetInfo = mapEntry.getValue();
                GamePlayer gamePlayer = dataVo.getGamePlayer(playerId);
                BetTableInfo betTableInfo = new BetTableInfo();
                betTableInfo.betIdx = mapEntry.getKey();
                //计算个人押注和总押注
                List<Integer> betList = playerBetInfo.get(playerId);
                long playerBet = betList == null ? 0 : betList.stream().mapToInt(Integer::intValue).sum();
                long totalBet = 0;
                List<BetPlayerChip> betGoldList = new ArrayList<>();
                for (Map.Entry<Long, List<Integer>> longLongEntry : playerBetInfo.entrySet()) {
                    int playerTotalBet = longLongEntry.getValue().stream().mapToInt(Integer::intValue).sum();
                    for (Integer betValue : longLongEntry.getValue()) {
                        //筹码值和皮肤
                        BetPlayerChip betPlayerChip = new BetPlayerChip();
                        betPlayerChip.chipValue = betValue;
                        betPlayerChip.chipId = gamePlayer.getChipsId();
                        betTableInfo.betGoldList.add(betPlayerChip);
                    }
                    totalBet += playerTotalBet;
                }
                betTableInfo.playerBetTotal = playerBet;
                betTableInfo.betIdxTotal = totalBet;
                betTableInfo.betGoldList = betGoldList;
                tableAreaInfos.add(betTableInfo);
            }
            notifyLoongTigerWarInfo.tableAreaInfos = tableAreaInfos;
        }
        //添加结算信息
        if (gamePhase == EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            notifyLoongTigerWarInfo.settleInfos = dataVo.getCurrentSettleInfo();
        }
        //押分列表
        notifyLoongTigerWarInfo.betPointList = dataVo.getRoomCfg().getBetList();
        notifyLoongTigerWarInfo.playerInfos = TableMessageBuilder.buildPlayerInfoOnTable(dataVo);
        notifyLoongTigerWarInfo.totalPlayerNum = dataVo.getGamePlayerMap().size();
        notifyLoongTigerWarInfo.maxChipOnTable =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.MAX_CHIP_ON_TABLE).getIntValue();
        return notifyLoongTigerWarInfo;
    }
}
