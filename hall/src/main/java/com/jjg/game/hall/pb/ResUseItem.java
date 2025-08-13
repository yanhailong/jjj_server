package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/11 15:45
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_USE_ITEM,resp = true)
@ProtoDesc("使用道具返回")
public class ResUseItem extends AbstractResponse {
    public ResUseItem(int code) {
        super(code);
    }
}
