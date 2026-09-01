package com.jjg.game.activity.buyOneGetSeven.message;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_BUY_ONE_GET_SEVEN_CLAIM_REWARDS, resp = true)
@ProtoDesc("响应买一送七领取奖励")
public class ResBuyOneGetSevenClaimRewards extends AbstractResponse {
    @ProtoDesc("活动ID")
    public long activityId;
    @ProtoDesc("本次领取的奖励配置ID")
    public int detailId;
    @ProtoDesc("本次领取的奖励")
    public List<ItemInfo> infoList;
    @ProtoDesc("最新活动奖励状态")
    public List<BuyOneGetSevenDetailInfo> activityData;

    public ResBuyOneGetSevenClaimRewards(int code) {
        super(code);
    }
}
