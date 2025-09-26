package com.jjg.game.activity.officialawards.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("官方派奖详情信息")
public class OfficialAwardsDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("类型 1初级 2中级 3高级")
    public int type;
    @ProtoDesc("消耗")
    public int costNum;
}
