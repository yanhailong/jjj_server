package com.jjg.game.table.loongtigerwar.room.manager;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.res.BetTableInfo;
import com.jjg.game.table.common.message.res.TablePlayerInfo;
import com.jjg.game.table.loongtigerwar.gamephase.LoongTigerWarBetPhase;
import com.jjg.game.table.loongtigerwar.gamephase.LoongTigerWarReadyPhase;
import com.jjg.game.table.loongtigerwar.gamephase.LoongTigerWarSettlementPhase;
import com.jjg.game.table.loongtigerwar.message.resp.NotifyLoongTigerWarInfo;
import com.jjg.game.table.loongtigerwar.message.resp.NotifyLoongTigerWarSettleInfo;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;

import java.util.*;

/**
 * 百家乐游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.RED_BLACK_WAR)
public class LoongTigerWarRoomGameController extends BaseTableGameController<LoongTigerWarGameDataVo> {

    public LoongTigerWarRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.RED_BLACK_WAR;
    }

    /**
     * 百家乐的房间不会停止
     *
     * @return 是否停止
     */
    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
        gamePhases.add(new LoongTigerWarReadyPhase(this));
        gamePhases.add(new LoongTigerWarBetPhase(this));
        gamePhases.add(new LoongTigerWarSettlementPhase(this));
        return gamePhases;
    }


    @Override
    protected LoongTigerWarGameDataVo copyRoomDataVo(GameDataVo<Room_BetCfg> roomData) {
        return new LoongTigerWarGameDataVo(roomData.getRoomCfg());
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public void sendRoomInitInfo(PlayerController playerController) {
        //发送房间信息
        LoongTigerWarGameDataVo dataVo = getGameDataVo();
        NotifyLoongTigerWarInfo notifyRedBlackWarInfo = new NotifyLoongTigerWarInfo();
        //历史记录
        notifyRedBlackWarInfo.redBlackHistories = dataVo.getHistories();
        //金币最高的玩家(6人)
        notifyRedBlackWarInfo.playerInfos = TableMessageBuilder.buildTablePlayerInfo(dataVo.getFixPlayers(), gameDataVo);
        //阶段信息
        notifyRedBlackWarInfo.gamePhase = getCurrentGamePhase();
        //阶段结束时间
        notifyRedBlackWarInfo.tableCountDownTime = dataVo.getPhaseEndTime();
        //各区域押注信息
        Map<Integer, Map<Long, Long>> betInfoMap = dataVo.getBetInfo();
        if (!betInfoMap.isEmpty()) {
            List<BetTableInfo> tableAreaInfos = new ArrayList<>();
            //遍历押注信息
            for (Map.Entry<Integer, Map<Long, Long>> mapEntry : betInfoMap.entrySet()) {
                BetTableInfo betTableInfo = new BetTableInfo();
                betTableInfo.betIdx = mapEntry.getKey();
                long playerBet = 0;
                long totalBet = 0;
                //计算个人押注和总押注
                for (Map.Entry<Long, Long> longLongEntry : mapEntry.getValue().entrySet()) {
                    if (longLongEntry.getKey() == playerController.playerId()) {
                        playerBet += longLongEntry.getValue();
                    }
                    totalBet += longLongEntry.getValue();
                }
                betTableInfo.playerBetTotal = totalBet;
                betTableInfo.betIdxTotal = playerBet;
                betTableInfo.betGoldList = mapEntry.getValue().values().stream().map(Long::intValue).toList();
                tableAreaInfos.add(betTableInfo);
            }
            notifyRedBlackWarInfo.tableAreaInfos = tableAreaInfos;
        }
        //添加结算信息
        if (getCurrentGamePhase() == EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            NotifyLoongTigerWarSettleInfo warSettleInfo = gameDataVo.getCurrentSettleInfo();
            warSettleInfo.getGold = 0;
            notifyRedBlackWarInfo.settleInfos = warSettleInfo;
        }
        //发送给玩家
        sendMessage(playerController.playerId(), notifyRedBlackWarInfo);
    }


    @Override
    public void initGame() {


    }

}
