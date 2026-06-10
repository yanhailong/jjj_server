package com.jjg.game.social.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.social.constant.SocialConst;

import java.util.List;

/**
 * 黑名单操作返回。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SOCIAL, cmd = SocialConst.MsgBean.RES_BLACKLIST_OP, resp = true)
@ProtoDesc("黑名单操作返回")
public class ResBlacklistOp extends AbstractResponse {
    @ProtoDesc("操作 1.拉黑 2.移除 3.一键移除")
    public int op;
    @ProtoDesc("受影响的玩家id")
    public List<Long> playerIds;

    public ResBlacklistOp(int code) {
        super(code);
    }
}
