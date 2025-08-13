package com.jjg.game.hall.pb.friendroom.struct;

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
    public String playerHeadIcon;

    @ProtoDesc("玩家Vip等级")
    public int playerVipLevel;
}
