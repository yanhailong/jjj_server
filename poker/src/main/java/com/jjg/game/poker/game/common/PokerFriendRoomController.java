package com.jjg.game.poker.game.common;

import com.jjg.game.core.data.PokerFriendRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

/**
 * 扑克好友房，房间控制器
 *
 * @author 2CL
 */
public class PokerFriendRoomController extends AbstractFriendRoomController<Room_ChessCfg, PokerFriendRoom> {

    public PokerFriendRoomController(Class<? extends RoomPlayer> roomPlayerClazz, PokerFriendRoom room) {
        super(roomPlayerClazz, room);
    }

    @Override
    public void reloadRoomCfg() {
        // 重载配置表引用
        roomCfg = GameDataManager.getRoom_ChessCfg(room.getRoomCfgId());
        gameController.getGameDataVo().reloadRoomCfg();
    }
}
