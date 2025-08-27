package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.BaseFriendRoomPlayerInfo;

import java.util.List;

/**
 * 返回刷新房间好友列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_REFRESH_ROOM_FRIEND_LIST,
    resp = true
)
@ProtoDesc("刷新关注好友列表")
public class ResRefreshFollowedFriendList extends AbstractResponse {

    @ProtoDesc("好友关注列表")
    public List<BaseFriendRoomPlayerInfo> followedFriendList;

    @ProtoDesc("下一页分页下标,为-1时表示到了末页")
    public int pageIdx;

    @ProtoDesc("页大小")
    public int pageSize;

    @ProtoDesc("关注上限")
    public int maxFollowLimit;

    public ResRefreshFollowedFriendList(int code) {
        super(code);
    }
}
