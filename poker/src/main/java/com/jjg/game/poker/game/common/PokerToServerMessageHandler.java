package com.jjg.game.poker.game.common;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/6 14:09
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class PokerToServerMessageHandler extends CoreToServerMessageHandler {
}
