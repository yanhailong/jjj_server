package com.jjg.game.table.redblackwar.message;

import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.redblackwar.message.resp.NotifyRedBlackWarInfo;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 2CL
 */
public class RedBlackMessageBuilder {

    /**
     * 构建初始化信息
     */
    public static NotifyRedBlackWarInfo buildInitInfo(
        long playerId, RedBlackWarGameDataVo dataVo, EGamePhase gamePhase) {

        NotifyRedBlackWarInfo notifyRedBlackWarInfo = new NotifyRedBlackWarInfo();
        //历史记录
        notifyRedBlackWarInfo.redBlackHistories = dataVo.getHistories();
        //阶段信息
        notifyRedBlackWarInfo.gamePhase = gamePhase;
        //阶段结束时间
        notifyRedBlackWarInfo.tableCountDownTime = dataVo.getPhaseEndTime();
        //各区域押注信息
        Map<Integer, Map<Long, List<Integer>>> betInfoMap = dataVo.getBetInfo();
        if (!betInfoMap.isEmpty()) {
            List<BetTableInfo> tableAreaInfos = new ArrayList<>();
            //遍历押注信息
            for (Map.Entry<Integer, Map<Long, List<Integer>>> mapEntry : betInfoMap.entrySet()) {
                Map<Long, List<Integer>> playerBetInfo = mapEntry.getValue();
                BetTableInfo betTableInfo = new BetTableInfo();
                betTableInfo.betIdx = mapEntry.getKey();
                //计算个人押注和总押注
                List<Integer> betList = playerBetInfo.get(playerId);
                long playerBet = betList == null ? 0 : betList.stream().mapToInt(Integer::intValue).sum();
                long totalBet = 0;
                List<Integer> betGoldList = new ArrayList<>();
                for (Map.Entry<Long, List<Integer>> longLongEntry : playerBetInfo.entrySet()) {
                    int playerTotalBet = longLongEntry.getValue().stream().mapToInt(Integer::intValue).sum();
                    betGoldList.addAll(longLongEntry.getValue());
                    totalBet += playerTotalBet;
                }
                betTableInfo.playerBetTotal = playerBet;
                betTableInfo.betIdxTotal = totalBet;
                betTableInfo.betGoldList = betGoldList;
                tableAreaInfos.add(betTableInfo);
            }
            notifyRedBlackWarInfo.tableAreaInfos = tableAreaInfos;
        }
        //添加结算信息
        if (gamePhase == EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            notifyRedBlackWarInfo.settleInfos = dataVo.getCurrentSettleInfo();
        }
        //押分列表
        notifyRedBlackWarInfo.betPointList = dataVo.getRoomCfg().getBetList();
        notifyRedBlackWarInfo.playerInfos = TableMessageBuilder.buildPlayerInfoOnTable(dataVo);
        notifyRedBlackWarInfo.totalPlayerNum = dataVo.getGamePlayerMap().size();
        notifyRedBlackWarInfo.maxChipOnTable =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.MAX_CHIP_ON_TABLE).getIntValue();
        return notifyRedBlackWarInfo;
    }
}
