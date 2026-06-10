package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.ConversationInfo;

import java.util.List;

/**
 * 私聊会话列表返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_CONVERSATION_LIST, resp = true)
@ProtoDesc("私聊会话列表返回")
public class ResConversationList extends AbstractResponse {
    @ProtoDesc("会话列表")
    public List<ConversationInfo> list;

    public ResConversationList(int code) {
        super(code);
    }
}
