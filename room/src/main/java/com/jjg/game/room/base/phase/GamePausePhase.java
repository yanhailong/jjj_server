package com.jjg.game.room.base.phase;

import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.sampledata.bean.RoomCfg;


/**
 * 游戏暂停阶段
 *
 * @author 2CL
 */
public abstract class GamePausePhase<RC extends RoomCfg, G extends GameDataVo<RC>> extends AbstractRoomPhase<RC, G> {

    public GamePausePhase(AbstractPhaseGameController<RC, G> gameController) {
        super(gameController);
    }
}
