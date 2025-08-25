package com.jjg.game.common.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * RPC服务的数据载体
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.SESSION_TYPE,
    cmd = MessageConst.SessionConst.RPC_SERVICE_DATA_CARRIER,
    toPbFile = false
)
public class RpcServiceDataCarrierMessage {

    @ProtoDesc("请求ID")
    public long requestId;

    @ProtoDesc("服务类名")
    public String serviceClassName;

    @ProtoDesc("服务方法名")
    public String serviceMethodName;

    @ProtoDesc("调用参数数据，json数据 map数据集，param类型 <=> param具体的数据")
    public String parameterTypeWithData;

    @ProtoDesc("调用参数数据，json数据 返回消息，满足调用方法的返回类型")
    public String responseData;

    @Override
    public String toString() {
        return "发送消息：RpcServiceDataCarrierMessage{" +
            "requestId=" + requestId +
            ", serviceClassName='" + serviceClassName + '\'' +
            ", serviceMethodName='" + serviceMethodName + '\'' +
            ", parameterTypeWithData='" + parameterTypeWithData + '\'' +
            '}';
    }
}
