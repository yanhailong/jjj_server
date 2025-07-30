package com.jjg.game.table.common.gamephase;

import com.jjg.game.core.constant.Code;
import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.message.res.NotifyRoomReadyWait;

/**
 * 游戏开场等待
 *
 * @author 2CL
 */
public class WaitReadyPhase<RD extends GameDataVo<Room_BetCfg>> extends AbstractRoomPhase<Room_BetCfg, RD> {

    public WaitReadyPhase(AbstractGameController<Room_BetCfg, RD> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        NotifyRoomReadyWait notifyRoomReadyWait = new NotifyRoomReadyWait(Code.SUCCESS);
        notifyRoomReadyWait.roomId = gameDataVo.getRoomId();
        notifyRoomReadyWait.waitEndTime = gameDataVo.getPhaseEndTime();
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
}
