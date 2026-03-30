package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;

/**
 * @author 11
 * @date 2025/6/19 14:58
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = DollarExpressConstant.MsgBean.RES_CHOOSE_FREE_MODEL, resp = true)
@ProtoDesc("返回选择免费模式的游戏")
public class ResChooseFreeModel extends AbstractResponse {
    @ProtoDesc("选择类型  3.普通火车  4.黄金火车   5.免费游戏")
    public int status;
    @ProtoDesc("免费次数")
    public int freeCount;

    public ResChooseFreeModel(int code) {
        super(code);
    }
}
