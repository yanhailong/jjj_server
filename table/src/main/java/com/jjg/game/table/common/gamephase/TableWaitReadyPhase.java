package com.jjg.game.table.common.gamephase;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.base.AbstractMsgDealRoomPhase;
import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.base.IPhaseMsgAdapter;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.TableConstant;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.common.message.req.ReqBet;
import com.jjg.game.table.common.message.res.NotifyRoomReadyWait;

/**
 * 游戏开场等待
 *
 * @author 2CL
 */
public class TableWaitReadyPhase<T extends TableGameDataVo> extends AbstractMsgDealRoomPhase<Room_BetCfg, T, ReqBet> {

    public TableWaitReadyPhase(AbstractGameController<Room_BetCfg, T> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 通知房间等待消息
        notifyRoomReadyMessage();
        // 清除数据
        gameDataVo.clearRoundData();
    }

    /**
     * 通知房间进入ready消息
     */
    protected void notifyRoomReadyMessage() {
        NotifyRoomReadyWait notifyRoomReadyWait = new NotifyRoomReadyWait(Code.SUCCESS);
        notifyRoomReadyWait.roomId = gameDataVo.getRoomId();
        notifyRoomReadyWait.waitEndTime = gameDataVo.getPhaseEndTime();
        notifyRoomReadyWait.tablePlayerInfo =
            TableMessageBuilder.buildTablePlayerInfo(gameDataVo, TableConstant.ON_TABLE_PLAYER_NUM);
        notifyRoomReadyWait.totalPlayerNum = gameDataVo.getPlayerNum();
        // 发送进入等待时间的消息
        broadcastMsgToRoom(notifyRoomReadyWait);
    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {

    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {

    }

    @Override
    public void onPlayerHalfwayJoinPhase(GamePlayer gamePlayer) {

    }

    @Override
    public void onPlayerHalfwayExitPhase(GamePlayer gamePlayer) {

    }

    @Override
    public void phaseFinish() {

    }

    @Override
    public int getPhaseRunTime() {
        RoomCfg roomCfg = gameDataVo.getRoomCfg();
        return roomCfg.getStageTime().get(0);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.WAIT_READY;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public int reqMsgId() {
        return TableRoomMessageConstant.ReqMsgBean.REQ_BET;
    }

    @Override
    public void dealMsg(PlayerController playerController, ReqBet message) {
        // 等待阶段也可以进行押注
        IRoomPhase roomPhase = gameController.findRoomPhase(EGamePhase.BET);
        if (roomPhase instanceof IPhaseMsgAdapter<?> msgAdapter) {
            ((IPhaseMsgAdapter<ReqBet>) msgAdapter).dealMsg(playerController, message);
        }
    }
}
