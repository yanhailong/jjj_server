package com.jjg.game.poker.game.common;

import com.jjg.game.core.data.PokerRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

/**
 * poker类的房间控制器
 *
 * @author lm
 */
public class PokerRoomController extends AbstractRoomController<Room_ChessCfg, PokerRoom> {

    public PokerRoomController(Class<? extends RoomPlayer> roomPlayerClazz, PokerRoom room) {
        super(roomPlayerClazz, room);
    }

    @Override
    public void reloadRoomCfg() {
        // 重载配置表引用
        roomCfg = GameDataManager.getRoom_ChessCfg(room.getRoomCfgId());
        gameController.getGameDataVo().reloadRoomCfg();
    }
}
