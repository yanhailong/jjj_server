package com.jjg.game.social.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

import java.util.List;

/**
 * 赠送礼物(体力)。all=true 为一键赠送全部好友。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.REQ_SEND_GIFT)
@ProtoDesc("赠送礼物")
public class ReqSendGift extends AbstractMessage {
    @ProtoDesc("好友id列表")
    public List<Long> playerIds;
    @ProtoDesc("是否一键赠送全部好友")
    public boolean all;
}
