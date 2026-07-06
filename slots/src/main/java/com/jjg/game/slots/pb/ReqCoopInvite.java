package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.List;

/**
 * 请求发送协作房间邀请 (经社交频道投递)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.COOP_ROOM, cmd = SlotsConst.SlotsCommon.REQ_COOP_INVITE)
@ProtoDesc("请求发送协作房间邀请")
public class ReqCoopInvite extends AbstractMessage {
    @ProtoDesc("频道 1世界 3好友私聊 4联盟 (ChatChannelType.code)")
    public int channelCode;
    @ProtoDesc("好友私聊时的目标玩家id列表 (其他频道忽略)")
    public List<Long> targetIds;
}
