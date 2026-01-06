package com.jjg.game.activity.scratchcards.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2026/1/5 09:57
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_SCRATCH_CARDS_EXCHANGE, resp = true)
@ProtoDesc("响应刮刮乐请求兑换道具")
public class ResScratchCardsExchange extends AbstractResponse {
    @ProtoDesc("奖励列表")
    public List<ItemInfo> rewardList;

    public ResScratchCardsExchange(int code) {
        super(code);
    }
}
