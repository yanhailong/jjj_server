package com.jjg.game.slots.game.basketballSuperstar.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CHRISTMAS_NIGHT_TYPE, cmd = BasketballSuperstarConstant.MsgBean.REQ_POOL_INFO)
@ProtoDesc("请求奖池信息")
public class ReqBasketballSuperstarPoolInfo extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
