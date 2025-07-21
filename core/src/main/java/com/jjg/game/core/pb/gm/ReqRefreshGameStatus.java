package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/7/15 15:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.REQ_REFRESH_GAME_STATUS)
@ProtoDesc("gm请求刷新服务器状态")
public class ReqRefreshGameStatus {
    @ProtoDesc("请求参数（存日志）")
    public String cmdParam;
}
