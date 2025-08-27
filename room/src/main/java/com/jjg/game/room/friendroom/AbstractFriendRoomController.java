package com.jjg.game.room.friendroom;

import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.sampledata.bean.RoomCfg;

/**
 * @author 2CL
 */
public abstract class AbstractFriendRoomController<RC extends RoomCfg> extends AbstractRoomController<RC, FriendRoom> {

    public AbstractFriendRoomController(Class<? extends RoomPlayer> roomPlayerClazz, FriendRoom room) {
        super(roomPlayerClazz, room);
    }

    @Override
    public void continueGame() {
        super.continueGame();
        roomDao.doSave(room.getGameType(), room.getId(), (r) -> r.setStatus(0));
    }

    @Override
    public void pauseGame() {
        super.pauseGame();
        roomDao.doSave(room.getGameType(), room.getId(), (r) -> r.setStatus(1));
    }

    @Override
    public void stopGame() {
        super.stopGame();
    }

    @Override
    protected void checkRobotJoinRoom() {
    }
}
