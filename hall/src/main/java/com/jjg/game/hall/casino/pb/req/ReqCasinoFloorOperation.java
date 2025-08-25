package com.jjg.game.hall.casino.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/18 14:41
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_CASINO_FLOOR_OPERATION)
@ProtoDesc("请求楼层操作")
public class ReqCasinoFloorOperation {
    @ProtoDesc("赌场id")
    public int casinoId;
    @ProtoDesc("楼层id")
    public int floorId;
    @ProtoDesc("类型 2打扫楼层 3提前结束楼层打扫")
    public int type;
}
