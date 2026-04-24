package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.hulk.HulkConstant;

/**
 * @author 11
 * @date 2026/4/23
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HULK, cmd = HulkConstant.MsgBean.REQ_MINI_CAR)
@ProtoDesc("请求汽车小游戏")
public class ReqHulkMiniGameCar extends AbstractMessage {
    @ProtoDesc("汽车标记")
    public int index;
}
