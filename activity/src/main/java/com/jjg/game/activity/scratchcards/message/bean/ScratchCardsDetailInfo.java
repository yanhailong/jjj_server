package com.jjg.game.activity.scratchcards.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("刮刮乐详情信息")
public class ScratchCardsDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("类型 1奖励 2礼包")
    public int type;
    @ProtoDesc("含7个数")
    public int numOf7;
    @ProtoDesc("礼包购买金额")
    public double buyPrice;
}
