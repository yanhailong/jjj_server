package com.jjg.game.table.animals;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.Room;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.animals.data.AnimalsGameDataVo;
import com.jjg.game.table.common.BaseTableGameController;

import java.util.LinkedHashSet;

/**
 * @author 2CL
 */
public class AnimalsGameController extends BaseTableGameController<AnimalsGameDataVo> {

    public AnimalsGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        return null;
    }

    @Override
    protected AnimalsGameDataVo copyRoomDataVo(GameDataVo<Room_BetCfg> roomData) {
        return null;
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public EGameType gameControlType() {
        return null;
    }

    @Override
    public void initGame() {

    }
}
