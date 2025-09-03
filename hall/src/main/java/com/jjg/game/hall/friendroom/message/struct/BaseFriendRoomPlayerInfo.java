package com.jjg.game.hall.friendroom.message.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 房间好友信息
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("房间好友信息")
public class BaseFriendRoomPlayerInfo {

    @ProtoDesc("玩家ID")
    public long playerId;

    @ProtoDesc("玩家称号ID")
    public long playerTitleId;

    @ProtoDesc("玩家名")
    public String playerName;

    @ProtoDesc("玩家头像Icon")
    public int playerHeadIcon;

    @ProtoDesc("玩家Vip等级")
    public int playerVipLevel;

    @ProtoDesc("性别  0.女  1.男  2.其他")
    public byte gender;

    @ProtoDesc("国旗id")
    public int nationalId;

    @ProtoDesc("玩家等级")
    public int level;

    @ProtoDesc("是否置顶")
    public boolean isTopUp;

    @ProtoDesc("是否丢失好友关系,对方如果重置邀请码，此状态将为true")
    public boolean isLostFriendRelationship;

    @ProtoDesc("当前好友的房间数量")
    public int curRoomNum;

    @ProtoDesc("好友最大房间数量")
    public int maxRoomNum;

    @ProtoDesc("置顶时间")
    public long topUpTime;

    @ProtoDesc("添加此好友的时间")
    public long addTime;

    @ProtoDesc("好友邀请码")
    public int invitationCode;
}
