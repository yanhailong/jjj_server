package com.jjg.game.activity.growthfund.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.growthfund.message.bean.GrowthFundDetailInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/11/3 18:08
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_GROWTH_FUND_BUY_RESULT_INFO, resp = true)
@ProtoDesc("响应成长基金购买结果")
public class ResGrowthFundBuyResultInfo extends AbstractResponse {
    @ProtoDesc("修改的详细信息")
    public List<GrowthFundDetailInfo> detailInfo;
    @ProtoDesc("购买状态")
    public boolean isBuy;
    @ProtoDesc("获得道具")
    public List<ItemInfo> rewards;
    public ResGrowthFundBuyResultInfo(int code) {
        super(code);
    }
}
