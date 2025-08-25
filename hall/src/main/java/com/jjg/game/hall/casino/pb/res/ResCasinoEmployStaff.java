package com.jjg.game.hall.casino.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.casino.pb.bean.CasinoSimpleInfo;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/18 14:51
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CASINO_EMPLOY_STAFF, resp = true)
@ProtoDesc("响应雇佣职员")
public class ResCasinoEmployStaff extends AbstractResponse {
    @ProtoDesc("位置索引")
    public int index;
    @ProtoDesc("机台id")
    public long machineId;
    @ProtoDesc("职员id")
    public long staffId;
    @ProtoDesc("到期时间")
    public long endTime;
    @ProtoDesc("机台简要信息")
    public CasinoSimpleInfo simpleMachineInfo;
    public ResCasinoEmployStaff() {
        super(Code.SUCCESS);
    }
}
