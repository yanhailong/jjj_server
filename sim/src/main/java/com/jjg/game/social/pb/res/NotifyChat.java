package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.ChatMsgInfo;

/**
 * 新聊天消息下发 (世界/系统/联盟/私聊 共用)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.NOTIFY_CHAT, resp = true)
@ProtoDesc("新聊天消息")
public class NotifyChat extends AbstractResponse {
    @ProtoDesc("消息")
    public ChatMsgInfo msg;

    public NotifyChat(int code) {
        super(code);
    }
}
