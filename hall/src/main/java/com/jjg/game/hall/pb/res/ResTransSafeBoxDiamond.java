package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/18 15:19
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_TRANS_SAFE_BOX_DIAMOND)
@ProtoDesc("转移保险箱钻石返回")
public class ResTransSafeBoxDiamond extends AbstractResponse {
    public ResTransSafeBoxDiamond(int code) {
        super(code);
    }
}
