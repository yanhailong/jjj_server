package com.jjg.game.social.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * 领取好友赠送的礼物 (一键收送中的"收")。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.REQ_COLLECT_GIFT)
@ProtoDesc("领取好友赠礼")
public class ReqCollectGift extends AbstractMessage {
}
