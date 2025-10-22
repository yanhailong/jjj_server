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
    @ProtoDesc("剩余积分")
    public long remainPoints;
    @ProtoDesc("总奖池")
    public long totalPool;
    @ProtoDesc("剩余时间")
    public long remainTime;
    @ProtoDesc("单次消耗积分")
    public int costPoint;
    @ProtoDesc("开始时间")
    public OfficialAwardsStartInfo startInfos;
    @ProtoDesc("活动状态 1未开始 2进行中 3已结束")
    public int activityState;
    @ProtoDesc("转盘类型")
    public int turntableType;
    @ProtoDesc("显示类型 1每天 2根据号数")
    public int showType;
}
