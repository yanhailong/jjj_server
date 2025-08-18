package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/18 15:16
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_TRANS_SAFE_BOX_DIAMOND)
@ProtoDesc("请求转移保险箱钻石")
public class ReqTransSafeBoxDiamond extends AbstractMessage {
    public long value;
    @ProtoDesc("是否存入操作")
    public boolean deposit;
}
