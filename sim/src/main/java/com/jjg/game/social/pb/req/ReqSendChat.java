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
    @ProtoDesc("频道 1.世界 2.系统 3.私聊 4.联盟 5.房间")
    public int channel;
    @ProtoDesc("目标  channel=3时为玩家id  channel=4时为联盟id(0表示当前联盟)  channel=5时为房间id(0表示当前房间)")
    public long targetId;
    @ProtoDesc("内容")
    public String content;
}
