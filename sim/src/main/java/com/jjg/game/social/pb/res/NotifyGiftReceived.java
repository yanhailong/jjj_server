package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * 收到好友赠礼通知 (驱动一键收送红点)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.NOTIFY_GIFT_RECEIVED, resp = true)
@ProtoDesc("收到好友赠礼")
public class NotifyGiftReceived extends AbstractResponse {
    @ProtoDesc("赠送者id")
    public long senderId;
    @ProtoDesc("体力数量")
    public int amount;

    public NotifyGiftReceived(int code) {
        super(code);
    }
}
