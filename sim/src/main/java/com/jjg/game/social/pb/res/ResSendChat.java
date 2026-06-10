package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.ChatMsgInfo;

/**
 * 发送聊天返回 (回显已生成 id/时间的消息)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_SEND_CHAT, resp = true)
@ProtoDesc("发送聊天返回")
public class ResSendChat extends AbstractResponse {
    @ProtoDesc("消息(成功时返回)")
    public ChatMsgInfo msg;

    public ResSendChat(int code) {
        super(code);
    }
}
