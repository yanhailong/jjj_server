package com.jjg.game.table.common;

import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.TablePlayerGameData;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.res.NotifyTableRoomPlayerInfoChange;

/**
 * table类房间基类
 *
 * @author 2CL
 */
public abstract class BaseTableGameController<G extends TableGameDataVo> extends
    AbstractGameController<Room_BetCfg, G> {

    public BaseTableGameController(AbstractRoomController<Room_BetCfg, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    protected GamePlayer onPlayerJoinRoom(PlayerController playerController, boolean gameStartStatus) {
        GamePlayer gamePlayer = super.onPlayerJoinRoom(playerController, gameStartStatus);
        gamePlayer.setTableGameData(new TablePlayerGameData());
        // 通知场上玩家加入
        NotifyTableRoomPlayerInfoChange playerInfoChange =
            TableMessageBuilder.buildNotifyTableRoomPlayerInfoChange(playerController.playerId(), gameDataVo);
        roomController.broadcastToRoomAllPlayers(playerInfoChange);
        return gamePlayer;
    }

    @Override
    public CommonResult<Room> onPlayerLeaveRoom(PlayerController playerController) {
        CommonResult<Room> leaveRes = super.onPlayerLeaveRoom(playerController);
        // 通知场上玩家离开
        NotifyTableRoomPlayerInfoChange playerInfoChange =
            TableMessageBuilder.buildNotifyTableRoomPlayerInfoChange(playerController.playerId(), gameDataVo);
        roomController.broadcastToRoomAllPlayers(playerInfoChange);
        return leaveRes;
    }
}
