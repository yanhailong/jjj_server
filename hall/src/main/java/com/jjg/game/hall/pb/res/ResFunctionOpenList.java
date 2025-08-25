package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * 返回开放的功能列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.RES_FUNCTION_OPEN_LIST,
    resp = true
)
@ProtoDesc("返回开放的功能列表")
public class ResFunctionOpenList extends AbstractResponse {

    @ProtoDesc("功能ID列表")
    public List<Integer> openedFunctionIdList;

    public ResFunctionOpenList(int code) {
        super(code);
    }
}
