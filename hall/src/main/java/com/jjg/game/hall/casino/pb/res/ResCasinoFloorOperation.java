package com.jjg.game.hall.casino.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.casino.pb.bean.CasinoFloorInfo;

/**
 * @author lm
 * @date 2025/8/18 14:41
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CASINO_FLOOR_OPERATION,resp = true)
@ProtoDesc("响应楼层操作")
public class ResCasinoFloorOperation extends AbstractResponse {
    @ProtoDesc("赌场id")
    public int casinoId;
    @ProtoDesc("类型 1解锁楼层 2打扫楼层 3提前结束楼层打扫")
    public int type;
    @ProtoDesc("楼层信息")
    public CasinoFloorInfo casinoFloorInfo;

    public ResCasinoFloorOperation() {
        super(Code.SUCCESS);
    }
}
