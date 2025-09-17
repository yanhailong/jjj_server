package com.jjg.game.activity.levelpack.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("等级礼包详情信息")
public class PlayerLevelPackDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("剩余时间")
    public long remainTime;
    @ProtoDesc("礼包购买金额")
    public int buyPrice;
}
