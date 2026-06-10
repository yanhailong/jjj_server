package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * 发送好友申请返回 (成功=好友邀请已发送)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_ADD_FRIEND, resp = true)
@ProtoDesc("发送好友申请返回")
public class ResAddFriend extends AbstractResponse {
    public ResAddFriend(int code) {
        super(code);
    }
}
