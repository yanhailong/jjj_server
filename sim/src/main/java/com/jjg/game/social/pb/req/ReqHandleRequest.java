package com.jjg.game.social.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

import java.util.List;

/**
 * 处理好友申请 (同意/拒绝, 多个id即一键操作)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.REQ_HANDLE_REQUEST)
@ProtoDesc("处理好友申请")
public class ReqHandleRequest extends AbstractMessage {
    @ProtoDesc("申请人id列表(传多个=一键)")
    public List<Long> playerIds;
    @ProtoDesc("true同意 false拒绝")
    public boolean agree;
}
