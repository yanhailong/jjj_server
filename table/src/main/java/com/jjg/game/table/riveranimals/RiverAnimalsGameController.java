package com.jjg.game.table.riveranimals;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.riveranimals.data.RiverAnimalsGameDataVo;
import com.jjg.game.table.riveranimals.gamephase.RiverAnimalsBetPhase;
import com.jjg.game.table.riveranimals.gamephase.RiverAnimalsSettlementPhase;
import com.jjg.game.table.riveranimals.gamephase.RiverAnimalsTableWaitReadyPhase;
import com.jjg.game.table.riveranimals.message.NotifyRiverAnimalsTableInfo;
import com.jjg.game.table.riveranimals.message.RiverAnimalsMessageBuilder;

import java.util.LinkedHashSet;

/**
 * 鱼虾蟹游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.RIVER_ANIMALS, roomType = RoomType.BET_ROOM)
public class RiverAnimalsGameController extends BaseTableGameController<RiverAnimalsGameDataVo> {

    public RiverAnimalsGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        // 发送初始化数据
        NotifyRiverAnimalsTableInfo animalsTableInfo =
            RiverAnimalsMessageBuilder.notifyAnimalsTableInfo(playerController.playerId(),this, true);
        playerController.send(animalsTableInfo);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> roomPhases = new LinkedHashSet<>();
        roomPhases.add(new RiverAnimalsTableWaitReadyPhase(this));
        roomPhases.add(new RiverAnimalsBetPhase(this));
        roomPhases.add(new RiverAnimalsSettlementPhase(this));
        return roomPhases;
    }

    @Override
    protected RiverAnimalsGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new RiverAnimalsGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public EGameType gameControlType() {
        return EGameType.RIVER_ANIMALS;
    }
}
