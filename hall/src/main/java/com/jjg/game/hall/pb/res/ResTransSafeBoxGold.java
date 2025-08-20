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
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_TRANS_SAFE_BOX_GOLD)
@ProtoDesc("转移保险箱金币返回")
public class ResTransSafeBoxGold extends AbstractResponse {
    @ProtoDesc("金币")
    public long gold;
    @ProtoDesc("保险箱金币")
    public long safeBoxGold;

    public ResTransSafeBoxGold(int code) {
        super(code);
    }
}
