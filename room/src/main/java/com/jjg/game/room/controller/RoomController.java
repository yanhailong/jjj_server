package com.jjg.game.room.controller;

import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.RoomPlayer;

/**
 * @author 11
 * @date 2025/6/25 12:34
 */
public class RoomController<D extends AbstractRoomDao> extends AbstractRoomController<RoomPlayer,D> {
    public RoomController() {
        super(RoomPlayer.class);
    }
}
