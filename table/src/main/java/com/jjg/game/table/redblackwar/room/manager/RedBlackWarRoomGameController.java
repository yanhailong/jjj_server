package com.jjg.game.table.redblackwar.room.manager;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.redblackwar.gamephase.RedBlackWarBetPhase;
import com.jjg.game.table.redblackwar.gamephase.RedBlackWarSettlementPhase;
import com.jjg.game.table.redblackwar.gamephase.RedBlackWarWaitReadyPhase;
import com.jjg.game.table.redblackwar.message.resp.NotifyRedBlackWarInfo;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 红黑大战游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.RED_BLACK_WAR)
public class RedBlackWarRoomGameController extends BaseTableGameController<RedBlackWarGameDataVo> {

    public RedBlackWarRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.RED_BLACK_WAR;
    }

    /**
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
        gamePhases.add(new RedBlackWarWaitReadyPhase(this));
        gamePhases.add(new RedBlackWarBetPhase(this));
        gamePhases.add(new RedBlackWarSettlementPhase(this));
        return gamePhases;
    }


    @Override
    protected RedBlackWarGameDataVo copyRoomDataVo(GameDataVo<Room_BetCfg> roomData) {
        return new RedBlackWarGameDataVo(roomData.getRoomCfg());
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public void sendRoomInitInfo(PlayerController playerController) {
        //发送房间信息
        RedBlackWarGameDataVo dataVo = getGameDataVo();
        NotifyRedBlackWarInfo notifyRedBlackWarInfo = new NotifyRedBlackWarInfo();
        //历史记录
        notifyRedBlackWarInfo.redBlackHistories = dataVo.getHistories();
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
            notifyRedBlackWarInfo.settleInfos = gameDataVo.getCurrentSettleInfo();
        }
        //押分列表
        notifyRedBlackWarInfo.betPointList = gameDataVo.getRoomCfg().getBetList();
        notifyRedBlackWarInfo.playerInfos = TableMessageBuilder.buildTablePlayerInfo(gameDataVo);
        //发送给玩家
        sendMessage(playerController.playerId(), notifyRedBlackWarInfo);
    }


    @Override
    public void initGame() {

    }

}
