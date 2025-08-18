package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.struct.PackItemInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/11 14:53
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_GET_PACK,resp = true)
@ProtoDesc("返回背包数据")
public class ResGetPack extends AbstractResponse {
    @ProtoDesc("背包中的道具")
    public List<PackItemInfo> packItemInfos;

    public ResGetPack(int code) {
        super(code);
    }
}
