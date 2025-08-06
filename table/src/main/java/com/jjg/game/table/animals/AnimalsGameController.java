package com.jjg.game.table.animals;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.animals.data.AnimalsGameDataVo;
import com.jjg.game.table.animals.gamephase.AnimalsBetPhase;
import com.jjg.game.table.animals.gamephase.AnimalsSettlementPhase;
import com.jjg.game.table.animals.gamephase.AnimalsTableWaitReadyPhase;
import com.jjg.game.table.animals.message.AnimalsMessageBuilder;
import com.jjg.game.table.animals.message.NotifyAnimalsTableInfo;
import com.jjg.game.table.common.BaseTableGameController;

import java.util.LinkedHashSet;

/**
 * 飞禽走兽游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.BIRDS_ANIMAL)
public class AnimalsGameController extends BaseTableGameController<AnimalsGameDataVo> {

    public AnimalsGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
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
