package com.jjg.game.sim.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/5/25
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class SimToServerMessageHandler extends CoreToServerMessageHandler {
}
