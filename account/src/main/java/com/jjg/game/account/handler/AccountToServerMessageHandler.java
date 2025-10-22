package com.jjg.game.account.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/10/22 13:42
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class AccountToServerMessageHandler extends CoreToServerMessageHandler {
}
