package com.jjg.game.ploy.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import com.jjg.game.core.pb.gm.ReqRefreshGameStatus;
import com.jjg.game.ploy.manager.PloyFactoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/5/9
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class PloyToServerMessageHandler extends CoreToServerMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private PloyFactoryManager ployFactoryManager;
    @Autowired
    private MarsCurator marsCurator;

    @Command(MessageConst.ToServer.REQ_REFRESH_GAME_STATUS)
    public void reqRefreshGameStatus(ReqRefreshGameStatus req) {
        log.info("收到刷新游戏状态命令: {}", JSON.toJSONString(req));
        try {
            ployFactoryManager.refreshGameStatus();
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
