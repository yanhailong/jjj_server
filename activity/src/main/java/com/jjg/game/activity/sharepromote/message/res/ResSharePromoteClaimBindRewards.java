package com.jjg.game.activity.sharepromote.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/16 15:45
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_SHARE_PROMOTE_CLAIM_BIND_REWARDS, resp = true)
@ProtoDesc("领取绑定玩家奖励")
public class ResSharePromoteClaimBindRewards extends AbstractResponse {
    public ResSharePromoteClaimBindRewards(int code) {
        super(code);
    }
    @ProtoDesc("奖励信息")
    public ItemInfo infoList;
}
