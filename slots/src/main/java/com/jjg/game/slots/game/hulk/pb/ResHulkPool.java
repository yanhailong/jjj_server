package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.goldsnakefortune.GoldSnakeFortuneConstant;
import com.jjg.game.slots.game.hulk.HulkConstant;

/**
 * @author 11
 * @date 2025/9/12 11:02
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HULK, cmd = HulkConstant.MsgBean.RES_POOL_VALUE,resp = true)
@ProtoDesc("奖池返回")
public class ResHulkPool extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResHulkPool(int code) {
        super(code);
    }
}
