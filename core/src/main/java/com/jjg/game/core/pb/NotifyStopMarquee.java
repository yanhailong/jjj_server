package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/6 13:58
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
        cmd = MessageConst.CoreMessage.NOTICE_STOP_MARQUEE, resp = true)
@ProtoDesc("通知停止跑马灯信息")
public class NotifyStopMarquee extends AbstractNotice {
    public long id;
}
