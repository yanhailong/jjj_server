package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.pb.struct.ChatMsgInfo;

import java.util.List;

/**
 * 聊天历史返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_CHAT_HISTORY, resp = true)
@ProtoDesc("聊天历史返回")
public class ResChatHistory extends AbstractResponse {
    @ProtoDesc("频道")
    public int channel;
    @ProtoDesc("消息列表(时间正序)")
    public List<ChatMsgInfo> list;
    @ProtoDesc("下一页游标(0表示无更早, 私聊用)")
    public long cursor;

    public ResChatHistory(int code) {
        super(code);
    }
}
