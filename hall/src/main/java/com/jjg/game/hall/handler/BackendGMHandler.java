package com.jjg.game.hall.handler;

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
 * 后台gm命令处理hallder
 *
 * @author lm
 * @date 2025/7/15 16:23
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class BackendGMHandler extends CoreToServerMessageHandler {

    @Autowired
    private HallService hallService;

    @Autowired
    private CoreLogger coreLogger;

    @Command(MessageConst.ToServer.REQ_REFRESH_GAME_STATUS)
    public void reqRefreshGameStatus(ReqRefreshGameStatus req) {
        log.info("收到刷新游戏状态命令: {}", req);
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
