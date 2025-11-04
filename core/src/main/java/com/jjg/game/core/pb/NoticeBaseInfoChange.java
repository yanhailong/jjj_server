package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/6/11 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.NOTICE_BASE_INFO_CHANGE,resp = true)
@ProtoDesc("推送玩家基础信息变化")
public class NoticeBaseInfoChange extends AbstractNotice {
    @ProtoDesc("vip等级")
    public int vipLevel;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("等级经验")
    public long levelExp;
}
