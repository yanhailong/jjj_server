package com.jjg.game.room.friendroom;

import com.jjg.game.core.data.Room;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.sampledata.bean.RoomCfg;


/**
 * 开房类型的游戏控制器
 *
 * @author 2CL
 */
public abstract class AbstractFriendGameController<RC extends RoomCfg, G extends GameDataVo<RC>>
    extends AbstractGameController<RC, G> {

    public AbstractFriendGameController(AbstractRoomController<RC, ? extends Room> roomController) {
        super(roomController);
    }
}
