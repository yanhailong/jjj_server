package com.jjg.game.slots.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.dollarexpress.pb.ReqConfigInfo;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/7/23 17:31
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class ToServerMessageHandler {
    /**
     * 结果库变更
     *
     * @param playerController
     * @param req
     */
    @Command(MessageConst.ToServer.NOTICE_SLOTS_LIB_CHANGE)
    public void reqConfigInfo(PlayerController playerController, ReqConfigInfo req) {

    }
}
