package com.jjg.game.poker.game.texas.room;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

/**
 * @author lm
 */
@GameController(gameType = EGameType.TEXAS, roomType = RoomType.POKER_TEAM_UP_ROOM)
public class TexasFriendGameController extends TexasGameController {

    public TexasFriendGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        super(roomController);
    }
}
