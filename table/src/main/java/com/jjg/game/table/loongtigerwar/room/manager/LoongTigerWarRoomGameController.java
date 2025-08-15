package com.jjg.game.table.loongtigerwar.room.manager;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.loongtigerwar.gamephase.LoongTigerWarBetPhase;
import com.jjg.game.table.loongtigerwar.gamephase.LoongTigerWarReadyPhaseTable;
import com.jjg.game.table.loongtigerwar.gamephase.LoongTigerWarSettlementPhase;
import com.jjg.game.table.loongtigerwar.message.resp.NotifyLoongTigerWarInfo;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 龙虎斗游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.LOONG_TIGER_WAR)
public class LoongTigerWarRoomGameController extends BaseTableGameController<LoongTigerWarGameDataVo> {

    public LoongTigerWarRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.LOONG_TIGER_WAR;
    }

    /**
     * @return 是否停止
     */
    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
        gamePhases.add(new LoongTigerWarReadyPhaseTable(this));
        gamePhases.add(new LoongTigerWarBetPhase(this));
        gamePhases.add(new LoongTigerWarSettlementPhase(this));
        return gamePhases;
    }


    @Override
    protected LoongTigerWarGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new LoongTigerWarGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        //发送房间信息
        LoongTigerWarGameDataVo dataVo = getGameDataVo();
        NotifyLoongTigerWarInfo notifyLoongTigerWarInfo = new NotifyLoongTigerWarInfo();
        //历史记录
        notifyLoongTigerWarInfo.histories = dataVo.getHistories();
        //阶段信息
        notifyLoongTigerWarInfo.gamePhase = getCurrentGamePhase();
        //阶段结束时间
        notifyLoongTigerWarInfo.tableCountDownTime = dataVo.getPhaseEndTime();
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
                List<Integer> betList = playerBetInfo.get(playerController.playerId());
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
            notifyLoongTigerWarInfo.tableAreaInfos = tableAreaInfos;
        }
        //添加结算信息
        if (getCurrentGamePhase() == EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            notifyLoongTigerWarInfo.settleInfos = gameDataVo.getCurrentSettleInfo();
        }
        //押分列表
        notifyLoongTigerWarInfo.betPointList = gameDataVo.getRoomCfg().getBetList();
        notifyLoongTigerWarInfo.playerInfos = TableMessageBuilder.buildPlayerInfoOnTable(gameDataVo);
        notifyLoongTigerWarInfo.totalPlayerNum = gameDataVo.getGamePlayerMap().size();
        notifyLoongTigerWarInfo.maxChipOnTable =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.MAX_CHIP_ON_TABLE).getIntValue();
        //发送给玩家
        broadcastToPlayers(
            RoomMessageBuilder.newBuilder().addPlayerId(playerController.playerId()).setData(notifyLoongTigerWarInfo));
    }

}
