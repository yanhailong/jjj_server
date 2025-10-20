package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbsNodeMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/25 11:35
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.RES_ACTIVITY_INFOS,
        resp = true, toPbFile = false)
@ProtoDesc("响应活动信息")
public class ResActivityInfos extends AbsNodeMessage {
    @ProtoDesc("活动信息json数据")
    public String activityJsonStr;
}
