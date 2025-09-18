package com.jjg.game.activity.levelpack.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/17 19:01
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = ActivityConstant.MsgBean.RES_PLAYER_LEVEL_CLAIM_REWARDS, resp = true)
@ProtoDesc("等级礼包购买结果")
public class ResPlayerLevelClaimRewards extends AbstractResponse {
    @ProtoDesc("id")
    public  int id;
    @ProtoDesc("道具信息")
    public List<ItemInfo> itemInfos;

    public ResPlayerLevelClaimRewards(int code) {
        super(code);
    }
}
