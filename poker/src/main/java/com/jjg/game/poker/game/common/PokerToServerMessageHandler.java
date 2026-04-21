package com.jjg.game.poker.game.common;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import com.jjg.game.core.pb.gm.NotifyGenerateToSouthLib;
import com.jjg.game.core.pb.gm.ReqRefreshGameStatus;
import com.jjg.game.poker.game.tosouth.cardlib.ToSouthCardLibManager;
import com.jjg.game.room.manager.AbstractRoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * @author 11
 * @date 2025/8/6 14:09
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class PokerToServerMessageHandler extends CoreToServerMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(PokerToServerMessageHandler.class);

    @Autowired
    private ToSouthCardLibManager toSouthCardLibManager;
    @Autowired
    private AbstractRoomManager roomManager;

    @Command(MessageConst.ToServer.NOTICE_GENERATE_TO_SOUTH_LIB)
    public void generateToSouthLib(NotifyGenerateToSouthLib req) {
        log.info("收到生成南方前进牌库请求 count={}", req.count);
        // 异步执行，避免阻塞消息处理线程
        CompletableFuture.runAsync(() -> {
            try {
                toSouthCardLibManager.generateCardLib(req.count);
            } catch (Exception e) {
                log.error("生成南方前进牌库异常", e);
            }
        });
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
