package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.BaseFriendRoomPlayerInfo;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBaseData;

import java.util.List;

/**
 * 返回好友房面板数据
 *
 * @author 2CL
 */

@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_FRIENDS_ROOM_PANEL_DATA,
    resp = true
)
@ProtoDesc("返回好友房面板数据")
public class NotifyFriendRoomPanelData extends AbstractNotice {

    @ProtoDesc("关注的玩家信息")
    public List<BaseFriendRoomPlayerInfo> roomFriendInfos;

    @ProtoDesc("当前牌局数")
    public int curTableNum;

    @ProtoDesc("最大牌局数")
    public int maxTableNum;

    @ProtoDesc("在牌桌上的玩家数量")
    public int playerNumOnTable;

    @ProtoDesc("最大玩家数量")
    public int maxPlayerNumOnTable;

    @ProtoDesc("房间基础信息列表，玩家自己的")
    public List<FriendRoomBaseData> roomBaseDataList;

    @ProtoDesc("玩家的邀请码")
    public int invitationCode;

    @ProtoDesc("邀请码剩余重置次数")
    public int invitationCodeResetRemainingTimes;

    @ProtoDesc("邀请码总共可以重置的次数")
    public int invitationCodeResetTotalTimes;
}
