package com.jjg.game.hall.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/12 9:25
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class HallToServerMessageHandler extends CoreToServerMessageHandler {
}
