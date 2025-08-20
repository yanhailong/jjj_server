package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/6 13:58
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
        cmd = MessageConst.CoreMessage.NOTICE_MARQUEE, resp = true)
@ProtoDesc("通知跑马灯信息")
public class NotifyMarquee extends AbstractNotice{
    @ProtoDesc("跑马灯信息")
    public MarqueeInfo marqueeInfo;
}
