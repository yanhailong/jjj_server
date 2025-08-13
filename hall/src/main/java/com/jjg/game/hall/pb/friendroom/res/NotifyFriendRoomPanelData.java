package com.jjg.game.hall.pb.friendroom.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.friendroom.struct.BaseFriendRoomPlayerInfo;

import java.util.List;

/**
 * 返回好友房面板数据
 *
 * @author 2CL
 */

@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.RES_FRIENDS_ROOM_PANEL_DATA,
    resp = true
)
@ProtoDesc("返回好友房面板数据")
public class NotifyFriendRoomPanelData extends AbstractResponse {

    @ProtoDesc("关注的玩家信息")
    public List<BaseFriendRoomPlayerInfo> roomFriendInfos;

    @ProtoDesc("最大玩家数量")
    public int maxPlayerNum;

    @ProtoDesc("当前牌局数")
    public int curTableNum;

    @ProtoDesc("最大牌局数")
    public int maxTableNum;

    @ProtoDesc("在牌桌上的玩家数量")
    public int playerNumOnTable;

    @ProtoDesc("最大玩家数量")
    public int maxPlayerNumOnTable;


    public NotifyFriendRoomPanelData(int code) {
        super(code);
    }
}
