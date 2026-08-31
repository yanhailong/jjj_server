package com.jjg.game.core.pb.reddot;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import java.util.List;

/** 客户端实际查看后上报，不用于领取奖励或清除可操作数量。 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
        cmd = MessageConst.CoreMessage.REQ_MARK_RED_DOT_READ)
@ProtoDesc("确认已查看红点内容，服务器使用NotifyRedDot推送最新状态")
public class ReqMarkRedDotRead extends AbstractMessage {
    @ProtoDesc("红点所属模块，必须指定")
    public RedDotDetails.RedDotModule module;
    @ProtoDesc("已查看的子模块，必须指定；不支持清除整个模块")
    public int submodule;
    @ProtoDesc("实际查看的实体ID列表；雇员传雇员ID，羁绊传羁绊ID；每日查看类不填")
    public List<Integer> entityIds;
}
