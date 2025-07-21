package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/6/11 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.NOTICE_MONEY_CHANGE,resp = true)
@ProtoDesc("推送金钱变化")
public class NoticeBaseInfoChange extends AbstractNotice{
    @ProtoDesc("金币")
    public long gold;
    @ProtoDesc("钻石")
    public long diamond;
    @ProtoDesc("vip等级")
    public int vipLevel;
}
