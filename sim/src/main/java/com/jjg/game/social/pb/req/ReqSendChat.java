package com.jjg.game.social.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * 发送聊天 (按 channel 区分世界/私聊/联盟…)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.REQ_SEND_CHAT)
@ProtoDesc("发送聊天")
public class ReqSendChat extends AbstractMessage {
    @ProtoDesc("频道 1世界2系统3私聊4联盟5房间")
    public int channel;
    @ProtoDesc("目标玩家id(私聊用)")
    public long targetId;
    @ProtoDesc("内容")
    public String content;
}
