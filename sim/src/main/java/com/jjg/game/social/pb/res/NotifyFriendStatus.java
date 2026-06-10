package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * 好友状态变更通知 (登录/登出/切场景时推送给在线好友)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.NOTIFY_FRIEND_STATUS, resp = true)
@ProtoDesc("好友状态变更")
public class NotifyFriendStatus extends AbstractResponse {
    @ProtoDesc("好友id")
    public long playerId;
    @ProtoDesc("状态 0.离线 1.在线 2.游戏中")
    public int status;

    public NotifyFriendStatus(int code) {
        super(code);
    }
}
