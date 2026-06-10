package com.jjg.game.social.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

import java.util.List;

/**
 * 删除好友 (多个id即一键删除)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.REQ_DELETE_FRIEND)
@ProtoDesc("删除好友")
public class ReqDeleteFriend extends AbstractMessage {
    @ProtoDesc("好友id列表(传多个=一键)")
    public List<Long> playerIds;
}
