package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求创建好友房
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_CREAT_FRIENDS_ROOM
)
@ProtoDesc("请求创建好友房")
public class ReqCreateFriendsRoom  extends AbstractMessage {

    @ProtoDesc("请求使用的道具ID 金币和券ID")
    public int itemId;

    @ProtoDesc("道具数量")
    public int itemNum;

    @ProtoDesc("房间配置ID，场次ID")
    public int roomCfgId;

    @ProtoDesc("申请开房时长")
    public int timeOfOpenRoom;

    @ProtoDesc("是否自动续费")
    public boolean autoRenewal;

    @ProtoDesc("庄家准备金")
    public boolean predictCostGoldNum;

    @ProtoDesc("房间名")
    public String roomAliasName;
}
