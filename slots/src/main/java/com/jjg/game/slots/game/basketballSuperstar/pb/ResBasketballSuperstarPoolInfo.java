package com.jjg.game.slots.game.basketballSuperstar.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BASKETBALL_SUPERSTAR, cmd = BasketballSuperstarConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("返回奖池信息")
public class ResBasketballSuperstarPoolInfo extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResBasketballSuperstarPoolInfo(int code) {
        super(code);
    }
}
