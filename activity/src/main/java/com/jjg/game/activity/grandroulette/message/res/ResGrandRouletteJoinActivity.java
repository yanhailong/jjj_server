package com.jjg.game.activity.grandroulette.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/4 13:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_GRAND_ROULETTE_JOIN_ACTIVITY, resp = true)
@ProtoDesc("大转盘领取活动奖励")
public class ResGrandRouletteJoinActivity extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("道具信息")
    public ItemInfo infoList;
    @ProtoDesc("随机中的索引")
    public int index;
    @ProtoDesc("剩余次数")
    public int remainTimes;
    @ProtoDesc("当前金币")
    public long currentGold;
    @ProtoDesc("目标金币")
    public long targetGold;
    @ProtoDesc("结束时间")
    public long endTime;
    public ResGrandRouletteJoinActivity(int code) {
        super(code);
    }
}
