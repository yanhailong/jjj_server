package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

import java.util.List;

/**
 * 删除好友返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_DELETE_FRIEND, resp = true)
@ProtoDesc("删除好友返回")
public class ResDeleteFriend extends AbstractResponse {
    @ProtoDesc("已删除的好友id")
    public List<Long> removedIds;

    public ResDeleteFriend(int code) {
        super(code);
    }
}
