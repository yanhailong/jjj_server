package com.jjg.game.table.common;

import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.sample.bean.Room_BetCfg;

/**
 * 对战类的房间控制器
 *
 * @author 2CL
 */
public class TableRoomController extends AbstractRoomController<Room_BetCfg, BetTableRoom> {

    public TableRoomController(Class<? extends RoomPlayer> roomPlayerClazz, BetTableRoom room) {
        super(roomPlayerClazz, room);
    }
}
