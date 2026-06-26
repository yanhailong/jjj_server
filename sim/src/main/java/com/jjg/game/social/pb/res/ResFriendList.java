package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.FriendInfo;

import java.util.List;

/**
 * 好友列表返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_FRIEND_LIST, resp = true)
@ProtoDesc("好友列表返回")
public class ResFriendList extends AbstractResponse {
    @ProtoDesc("好友列表(按 在线>游戏中>离线 排序)")
    public List<FriendInfo> friends;
    @ProtoDesc("好友数量上限")
    public int friendLimit;
    @ProtoDesc("在线好友数")
    public int onlineCount;
    @ProtoDesc("每天赠送好友限制")
    public int sendFriendLimit;
    @ProtoDesc("每人每天限制赠送次数")
    public int sendCountLimit;
    @ProtoDesc("今日已赠送总次数")
    public int hasSendCountToady;

    public ResFriendList(int code) {
        super(code);
    }
}
