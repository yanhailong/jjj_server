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
    @ProtoDesc("领取的道具id")
    public int itemId;
    @ProtoDesc("本次领取的道具总数量")
    public long gainCount;

    public ResCollectGift(int code) {
        super(code);
    }
}
