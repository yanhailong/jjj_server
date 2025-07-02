package com.jjg.game.table.common;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.TablePlayerGameData;
import com.jjg.game.room.sample.bean.RoomCfg;

/**
 * table类房间基类
 *
 * @author 2CL
 */
public abstract class BaseTableGameController<RC extends RoomCfg, G extends GameDataVo<RC>> extends AbstractGameController<RC, G> {

    public BaseTableGameController(AbstractRoomController<RC, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    protected GamePlayer onPlayerJoinRoom(PlayerController playerController, boolean gameStartStatus) {
        GamePlayer gamePlayer = super.onPlayerJoinRoom(playerController, gameStartStatus);
        gamePlayer.setTableGameData(new TablePlayerGameData());
        return gamePlayer;
    }
}
