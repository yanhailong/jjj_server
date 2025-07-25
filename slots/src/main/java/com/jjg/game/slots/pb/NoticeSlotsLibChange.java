package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/7/23 17:34
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_SLOTS_LIB_CHANGE,resp = true,toPbFile = false)
@ProtoDesc("通知slots节点，lib库变化")
public class NoticeSlotsLibChange {
    @ProtoDesc("游戏类型")
    public int gameType;
    @ProtoDesc("变化类型  0.生成结果库通知   1.配置表变化通知")
    public int changeType;
    @ProtoDesc("配置列表")
    public List<String> libCfgList;
}
