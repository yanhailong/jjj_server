package com.jjg.game.social.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 好友列表项。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage
@ProtoDesc("好友信息")
public class FriendInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("昵称")
    public String nick;
    @ProtoDesc("头像id")
    public int headImg;
    @ProtoDesc("头像框id")
    public int headFrame;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("状态 0离线1在线2游戏中")
    public int status;
    @ProtoDesc("离线时长(秒), 在线为0")
    public long offlineSeconds;
    @ProtoDesc("今日是否还可向其赠送")
    public boolean canGift;
    @ProtoDesc("其是否赠送了我待领礼物")
    public boolean hasPendingGift;
}
