package com.jjg.game.common.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 返回RPC服务数据
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.SESSION_TYPE,
    cmd = MessageConst.SessionConst.RPC_RES_SERVICE_DATA_CARRIER,
    toPbFile = false
)
public class RespRpcServiceData {

    @ProtoDesc("请求ID")
    public long requestId;

    @ProtoDesc("调用参数数据，json数据 返回消息，满足调用方法的返回类型")
    public String responseData;

    @ProtoDesc("是否成功")
    public boolean success;

    @Override
    public String toString() {
        return "RespRpcServiceData{" +
            "requestId=" + requestId +
            ", responseData='" + responseData + '\'' +
            ", success=" + success +
            '}';
    }
}
