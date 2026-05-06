package com.jjg.game.activity.grandroulette.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2026/4/22 18:03
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_GRAND_ROULETTE_HISTORY)
@ProtoDesc("大转盘历史记录")
public class ReqGrandRouletteHistory extends AbstractMessage {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("每页数目")
    public int size;
    @ProtoDesc("类型 1个人 2全服")
    public int type;
}
