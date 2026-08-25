package com.jjg.game.activity.bundleGiftPack.message;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_BUNDLE_GIFT_PACK, resp = true)
@ProtoDesc("响应集合礼包活动数据")
public class ResBundleGiftPack extends AbstractResponse {
    @ProtoDesc("活动列表信息")
    public List<BundleGiftPackDetailInfo> activityData;

    public ResBundleGiftPack(int code) {
        super(code);
    }
}
