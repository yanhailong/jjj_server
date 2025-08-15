package com.jjg.game.room.controller;

import com.jjg.game.core.data.Room;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.RoomCfg;

/**
 * 开房类型的游戏控制器
 *
 * @author 2CL
 */
public abstract class AbstractInviteGameController<RC extends RoomCfg, G extends GameDataVo<RC>>
    extends AbstractGameController<RC, G> {

    public AbstractInviteGameController(AbstractRoomController<RC, ? extends Room> roomController) {
        super(roomController);
    }
}
