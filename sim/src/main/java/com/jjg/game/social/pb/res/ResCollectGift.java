package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * 领取赠礼返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_COLLECT_GIFT, resp = true)
@ProtoDesc("领取赠礼返回")
public class ResCollectGift extends AbstractResponse {
    @ProtoDesc("本次领取的体力")
    public int gainStamina;
    @ProtoDesc("领取后的体力总量")
    public int totalStamina;

    public ResCollectGift(int code) {
        super(code);
    }
}
