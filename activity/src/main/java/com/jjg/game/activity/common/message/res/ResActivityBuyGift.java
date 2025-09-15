package com.jjg.game.activity.common.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/15 09:27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_ACTIVITY_BUY_GIFT, resp = true)
@ProtoDesc("活动购买礼包响应")
public class ResActivityBuyGift extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("道具信息")
    public List<ItemInfo> itemInfos;

    public ResActivityBuyGift(int code) {
        super(code);
    }
}
