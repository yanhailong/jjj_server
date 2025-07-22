package com.jjg.game.table.redblackwar.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.table.common.message.res.BetTableInfo;
import com.jjg.game.table.redblackwar.message.RedBlackWarMessageConstant;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/16 16:58
 */

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.RED_BLACK_WAR_TYPE,
        cmd = RedBlackWarMessageConstant.RespMsgBean.NOTIFY_BET_CHANGE, resp = true)
@ProtoDesc("玩家信息变化")
public class NotifyBetChange extends AbstractResponse {
    @ProtoDesc("下注的玩家id")
    public long playerId;
    @ProtoDesc("下注的索引")
    public int index;
    @ProtoDesc("变化的区域")
    public List<BetTableInfo> areaInfo;

    public NotifyBetChange() {
        super(Code.SUCCESS);
    }
}
