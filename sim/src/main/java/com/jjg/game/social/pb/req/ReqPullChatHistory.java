package com.jjg.game.social.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

/**
 * 拉取聊天历史。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.REQ_PULL_CHAT_HISTORY)
@ProtoDesc("拉取聊天历史")
public class ReqPullChatHistory extends AbstractMessage {
    @ProtoDesc("频道")
    public int channel;
    @ProtoDesc("目标玩家id(私聊用)")
    public long targetId;
    @ProtoDesc("分页游标(私聊用, 上一页最旧消息id; 0取最新一页)")
    public long cursor;
}
