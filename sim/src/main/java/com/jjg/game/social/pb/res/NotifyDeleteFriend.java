package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * @author 11
 * @date 2026/6/29
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.NOTIFY_DELETE_FRIEND, resp = true)
@ProtoDesc("通知删除好友")
public class NotifyDeleteFriend extends AbstractNotice {
    public long friendId;
}
