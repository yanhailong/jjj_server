package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 返回操作好友房结果
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_OPERATE_FRIEND_ROOM,
    resp = true
)
@ProtoDesc("返回操作好友房结果")
public class ResOperateFriendRoom extends AbstractResponse {

    @ProtoDesc("房间ID")
    public long roomId;

    @ProtoDesc("1. 运行中 2. 暂停中，3. 解散中")
    public int roomStatus;

    @ProtoDesc("暂停按钮下一次可用的时间，为0或者时间戳到期表示可用")
    public long nextPauseBtnOverdueTime;

    public ResOperateFriendRoom(int code) {
        super(code);
    }
}
