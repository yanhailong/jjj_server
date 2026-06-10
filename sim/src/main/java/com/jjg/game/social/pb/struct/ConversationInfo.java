package com.jjg.game.social.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 私聊会话项。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage
@ProtoDesc("私聊会话信息")
public class ConversationInfo {
    @ProtoDesc("对端玩家id")
    public long targetId;
    @ProtoDesc("昵称")
    public String nick;
    @ProtoDesc("头像id")
    public int headImg;
    @ProtoDesc("头像框id")
    public int headFrame;
    @ProtoDesc("对端是否在线")
    public boolean online;
    @ProtoDesc("最后一条消息内容")
    public String lastContent;
    @ProtoDesc("最后一条消息时间ms")
    public long lastTime;
    @ProtoDesc("未读数")
    public int unread;
}
