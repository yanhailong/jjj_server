package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * 删除私聊会话返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_DELETE_CONVERSATION, resp = true)
@ProtoDesc("删除私聊会话返回")
public class ResDeleteConversation extends AbstractResponse {
    @ProtoDesc("对端玩家id")
    public long targetId;

    public ResDeleteConversation(int code) {
        super(code);
    }
}
