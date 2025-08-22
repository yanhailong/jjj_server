package com.jjg.game.hall.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.pb.gm.ReqRefreshGameStatus;
import com.jjg.game.hall.service.HallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/12 9:25
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class HallToServerMessageHandler extends CoreToServerMessageHandler {
    @Autowired
    private HallService hallService;

    @Autowired
    private CoreLogger coreLogger;

    @Command(MessageConst.ToServer.REQ_REFRESH_GAME_STATUS)
    public void reqRefreshGameStatus(ReqRefreshGameStatus req) {
        log.info("收到刷新游戏状态命令: {}", JSON.toJSONString(req));
        String result = BackendGMCmd.Result.SUCCESS;
        try {
            hallService.refreshGameStatuses();
        } catch (Exception e) {
            log.error("", e);
            result = BackendGMCmd.Result.FAIL;
        }
        coreLogger.gmOrder(BackendGMCmd.CHANGE_GAME_STATUS + ":" + req.cmdParam, null, result);
    }
}
