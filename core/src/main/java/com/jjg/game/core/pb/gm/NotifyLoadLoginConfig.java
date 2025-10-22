package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/10/22 13:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTIFY_LOAD_LOGIN_CONFIG, resp = true,toPbFile = false)
@ProtoDesc("通知加载登录配置")
public class NotifyLoadLoginConfig extends AbstractNotice {
}
