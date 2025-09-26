package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/25 19:21
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTIFY_GAME_NODE_CHANGE, resp = true)
@ProtoDesc("通知轮播数据更新")
public class NotifyGameNodeChange {
    public int weight;
    //白名单ip
    public List<String> ips;
    //白名单id
    public List<String> ids;
}
