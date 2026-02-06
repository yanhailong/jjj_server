package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/11 14:53
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.RES_GET_PACK, resp = true)
@ProtoDesc("返回背包数据")
public class ResCoreGetPack extends AbstractResponse {
    @ProtoDesc("背包中的道具")
    public List<PackItemInfo> packItemInfos;

    public ResCoreGetPack(int code) {
        super(code);
    }
}
