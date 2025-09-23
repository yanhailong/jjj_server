package com.jjg.game.activity.officialawards.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:28
 */
@ProtobufMessage
@ProtoDesc("官方派奖类型活动信息")
public class OfficialAwardsActivity {
    @ProtoDesc("活动详细信息")
    public List<OfficialAwardsDetailInfo> detailInfos;
}
