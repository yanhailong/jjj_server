package com.jjg.game.table.birdsanimals;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.birdsanimals.data.AnimalsGameDataVo;
import com.jjg.game.table.birdsanimals.gamephase.AnimalsBetPhase;
import com.jjg.game.table.birdsanimals.gamephase.AnimalsSettlementPhase;
import com.jjg.game.table.birdsanimals.gamephase.AnimalsTableWaitReadyPhase;
import com.jjg.game.table.birdsanimals.message.AnimalsMessageBuilder;
import com.jjg.game.table.birdsanimals.message.NotifyAnimalsTableInfo;
import com.jjg.game.table.common.BaseFriendRoomTableGameController;

import java.util.LinkedHashSet;

/**
 * 飞禽走兽游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.BIRDS_ANIMAL, roomType = RoomType.BET_TEAM_UP_ROOM)
public class AnimalsFriendRoomGameController extends BaseFriendRoomTableGameController<AnimalsGameDataVo> {

    public AnimalsFriendRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        super.respRoomInitInfo(playerController);
        // 发送初始化数据
        NotifyAnimalsTableInfo animalsTableInfo =
            AnimalsMessageBuilder.notifyAnimalsTableInfo(this, true, playerController.playerId());
        playerController.send(animalsTableInfo);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> roomPhases = new LinkedHashSet<>();
        roomPhases.add(new AnimalsTableWaitReadyPhase(this));
        roomPhases.add(new AnimalsBetPhase(this));
        roomPhases.add(new AnimalsSettlementPhase(this));
        return roomPhases;
    }

    @Override
    protected AnimalsGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new AnimalsGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public EGameType gameControlType() {
        return EGameType.BIRDS_ANIMAL;
    }
}
