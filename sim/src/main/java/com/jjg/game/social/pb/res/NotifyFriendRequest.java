package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.RequestInfo;

/**
 * 收到新好友申请通知 (驱动管理页签红点)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.NOTIFY_FRIEND_REQUEST, resp = true)
@ProtoDesc("收到新好友申请")
public class NotifyFriendRequest extends AbstractResponse {
    @ProtoDesc("申请信息")
    public RequestInfo request;

    public NotifyFriendRequest(int code) {
        super(code);
    }
}
