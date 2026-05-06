package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/15 15:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.REQ_REFRESH_GLOBAL_CONFIG, resp = true, toPbFile = false)
@ProtoDesc("gm请求刷新全部表配置")
public class ReqRefreshGlobalConfig extends AbstractMessage {
    @ProtoDesc("刷新id列表")
    public List<Integer> refreshIds;
}
