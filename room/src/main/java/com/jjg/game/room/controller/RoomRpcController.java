package com.jjg.game.room.controller;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.handler.CoreRPCController;
import com.jjg.game.core.rpc.GmToRoomBridge;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/2/4
 */
@Component
public class RoomRpcController extends CoreRPCController implements GmToRoomBridge {
    @Autowired
    private RoomManager roomManager;

    @Override
    public int changeSvip(long playerId, int svip) {
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController =
                roomManager.getRoomControllerByPlayer(playerId);
        // 如果玩家不在房间中，或者不在好友房中
        if (roomController == null) {
            log.warn("设置玩家svip时，roomController 未找到 playerId = {}", playerId);
            return Code.NOT_FOUND;
        }

        PlayerController playerController = roomController.getPlayerController(playerId);
        if (playerController == null) {
            log.warn("设置玩家svip时，playerController 未找到 playerId = {}", playerId);
            return Code.NOT_FOUND;
        }

        GamePlayer gamePlayer = roomController.gameController.gameDataVo.getGamePlayer(playerId);
        if (gamePlayer == null) {
            log.warn("设置玩家svip时，gamePlayer 未找到 playerId = {}", playerId);
            return Code.NOT_FOUND;
        }

        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerController.getSession().getWorkId(), 0, new BaseHandler<String>() {
            @Override
            public void action() {
                try {
                    gamePlayer.setSvip(svip);
                    log.info("设置玩家svip成功 playerId = {},svip = {}", playerId, svip);
                } catch (Exception e) {
                    log.error("设置玩家svip异常, playerId={}, svip={}", playerId, svip, e);
                }
            }
        }.setHandlerParamWithSelf("playerSetSvip"));
        return Code.SUCCESS;
    }
}
