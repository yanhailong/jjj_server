package com.jjg.game.social.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 聊天消息 (下发结构)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage
@ProtoDesc("聊天消息")
public class ChatMsgInfo {
    @ProtoDesc("消息id")
    public long id;
    @ProtoDesc("频道 1.世界 2.系统 3.私聊 4.联盟 5.房间")
    public int channel;
    @ProtoDesc("频道子id 比如房间id，联盟id")
    public long channelSubId;
    @ProtoDesc("发送者id")
    public long fromId;
    @ProtoDesc("发送者昵称")
    public String fromNick;
    @ProtoDesc("发送者头像id")
    public int fromHeadImg;
    @ProtoDesc("发送者头像框id")
    public int fromHeadFrame;
    @ProtoDesc("接收者id(私聊)")
    public long toId;
    @ProtoDesc("内容")
    public String content;
    @ProtoDesc("发送时间ms")
    public long time;
}
