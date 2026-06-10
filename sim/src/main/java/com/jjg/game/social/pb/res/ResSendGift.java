package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

import java.util.List;

/**
 * 赠送礼物返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_SEND_GIFT, resp = true)
@ProtoDesc("赠送礼物返回")
public class ResSendGift extends AbstractResponse {
    @ProtoDesc("成功赠送的好友id")
    public List<Long> sentIds;

    public ResSendGift(int code) {
        super(code);
    }
}
