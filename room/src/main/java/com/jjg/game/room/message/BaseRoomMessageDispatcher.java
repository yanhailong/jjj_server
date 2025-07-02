package com.jjg.game.room.message;

import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GameDataVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 房间消息分发器
 *
 * @author 2CL
 */
public class BaseRoomMessageDispatcher {

    // log
    private static final Logger log = LoggerFactory.getLogger(BaseRoomMessageDispatcher.class);

    /**
     * 请求发送消息
     *
     * @param pfMessage message
     */
    public void dispatchMsg(PlayerController playerController, PFMessage pfMessage) {
        int cmd = pfMessage.cmd;
        AbstractRoomController<?, ?> roomController = (AbstractRoomController<?, ?>) playerController.getScene();
        if (roomController == null) {
            log.warn("玩家当前房间控制器为空，却请求消息：{}", cmd);
            return;
        }
        AbstractGameController<?, ? extends GameDataVo<?>> gameController = roomController.getGameController();
        if (gameController == null) {
            log.warn("玩家当前游戏控制器为空，却请求消息：{}", cmd);
            return;
        }
        // 处理游戏逻辑
        gameController.dispatchGamePhaseMsg(playerController, pfMessage);
    }
}
