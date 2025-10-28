package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/10/27 20:01
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_BIND_THIRD_ACCOUNT,resp = true)
@ProtoDesc("绑定第三方账号")
public class ResBindThirdAccount extends AbstractResponse {
    @ProtoDesc("获取道具")
    public List<ItemInfo> items;

    public ResBindThirdAccount(int code) {
        super(code);
    }
}
