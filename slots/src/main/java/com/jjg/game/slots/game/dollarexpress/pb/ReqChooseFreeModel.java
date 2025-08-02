package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;

/**
 * @author 11
 * @date 2025/6/19 14:58
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = DollarExpressConstant.MsgBean.REQ_CHOOSE_FREE_MODEL)
@ProtoDesc("请求选择免费模式的游戏")
public class ReqChooseFreeModel extends AbstractMessage {
    @ProtoDesc("选择类型  3.普通火车  4.黄金火车   5.免费游戏")
    public int status;
}
