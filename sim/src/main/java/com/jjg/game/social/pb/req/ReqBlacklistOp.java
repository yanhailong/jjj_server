package com.jjg.game.social.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

import java.util.List;

/**
 * 黑名单操作 (拉黑/移除/一键移除)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.REQ_BLACKLIST_OP)
@ProtoDesc("黑名单操作")
public class ReqBlacklistOp extends AbstractMessage {
    @ProtoDesc("操作 1拉黑2移除3一键移除")
    public int op;
    @ProtoDesc("目标玩家id列表(一键移除可不填)")
    public List<Long> playerIds;
}
