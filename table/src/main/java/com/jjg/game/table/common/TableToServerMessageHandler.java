package com.jjg.game.table.common;

import com.alibaba.fastjson.JSON;
import com.jjg.game.activity.grandroulette.controller.GrandRouletteController;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import com.jjg.game.core.pb.gm.ReqRefreshGameStatus;
import com.jjg.game.core.pb.gm.ReqRefreshGlobalConfig;
import com.jjg.game.room.manager.AbstractRoomManager;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/6 14:09
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class TableToServerMessageHandler extends CoreToServerMessageHandler {
    private final AbstractRoomManager roomManager;
    private final GrandRouletteController grandRouletteController;

    public TableToServerMessageHandler(AbstractRoomManager roomManager, GrandRouletteController grandRouletteController) {
        this.roomManager = roomManager;
        this.grandRouletteController = grandRouletteController;
    }

    @Command(MessageConst.ToServer.REQ_REFRESH_GLOBAL_CONFIG)
    public void reqRefreshGameConfig(ReqRefreshGlobalConfig req) {
        log.info("收到刷新游戏全部配置命令: {}", JSON.toJSONString(req));
        try {
            if (req.refreshIds.contains(GlobalSampleConstantId.GRAND_ROULETTE_128) ||
                    req.refreshIds.contains(GlobalSampleConstantId.GRAND_ROULETTE_131)) {
                grandRouletteController.reloadConfig();
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Command(MessageConst.ToServer.REQ_REFRESH_GAME_STATUS)
    public void reqRefreshGameStatus(ReqRefreshGameStatus req) {
        log.info("收到刷新游戏状态命令: {}", JSON.toJSONString(req));
        try {
            roomManager.refreshGameStatus();
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
