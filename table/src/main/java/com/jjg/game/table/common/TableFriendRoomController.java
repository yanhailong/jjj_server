package com.jjg.game.table.common;

import com.jjg.game.core.data.BetFriendRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.Room_BetCfg;

/**
 * 好友房
 *
 * @author 2CL
 */
public class TableFriendRoomController extends AbstractRoomController<Room_BetCfg, BetFriendRoom> {

    public TableFriendRoomController(Class<? extends RoomPlayer> roomPlayerClazz, BetFriendRoom room) {
        super(roomPlayerClazz, room);
    }

    @Override
    public void reloadRoomCfg() {
        // 重载配置表引用
        roomCfg = GameDataManager.getRoom_BetCfg(room.getRoomCfgId());
        gameController.getGameDataVo().reloadRoomCfg();
    }
}
