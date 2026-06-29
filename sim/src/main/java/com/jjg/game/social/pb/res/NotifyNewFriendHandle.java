package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.FriendInfo;

/**
 * @author 11
 * @date 2026/6/29
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.NOTIFY_NEW_FRIEND_HANDLE, resp = true)
@ProtoDesc("通知申请好友的处理结果")
public class NotifyNewFriendHandle extends AbstractNotice {
    public FriendInfo friendInfo;
    public boolean agree;
}
