package com.jjg.game.activity.sharepromote.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.sharepromote.message.bean.SharePromoteRewardsRecode;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/16 15:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_SHARE_PROMOTE_CLAIM_PROFIT_REWARD, resp = true)
@ProtoDesc("领取收奖励")
public class ResSharePromoteClaimProfitReward extends AbstractResponse {
    @ProtoDesc("记录")
    public List<SharePromoteRewardsRecode> recodes;
    @ProtoDesc("获得道具信息")
    public ItemInfo itemInfo;

    public ResSharePromoteClaimProfitReward(int code) {
        super(code);
    }
}
