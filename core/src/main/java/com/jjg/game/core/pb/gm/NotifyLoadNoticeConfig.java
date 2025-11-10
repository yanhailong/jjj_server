package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/11/10 10:42
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTIFY_LOAD_NOTICE_LIST, resp = true, toPbFile = false)
@ProtoDesc("通知加载公告配置")
public class NotifyLoadNoticeConfig extends AbstractNotice {
}
