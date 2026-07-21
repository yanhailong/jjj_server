package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 弃牌阶段结果通知，DESIGN.md 8.11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_DISCARD_RESULT, resp = true)
@ProtoDesc("斗仙牌通知弃牌结果")
public class NotifyDouXianDiscardResult extends AbstractNotice {
    @ProtoDesc("弃牌玩家id")
    public long playerId;
    @ProtoDesc("是否选择了不弃")
    public boolean noDiscard;
    @ProtoDesc("弃置的牌数量")
    public int discardCount;
    @ProtoDesc("是否所有玩家都已完成弃牌(true时客户端应进入下一回合)")
    public boolean allDiscarded;
}
