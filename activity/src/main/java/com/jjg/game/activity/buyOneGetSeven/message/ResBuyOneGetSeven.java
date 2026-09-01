package com.jjg.game.activity.buyOneGetSeven.message;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_BUY_ONE_GET_SEVEN, resp = true)
@ProtoDesc("响应买一送七活动数据")
public class ResBuyOneGetSeven extends AbstractResponse {
    @ProtoDesc("活动ID")
    public long activityId;
    @ProtoDesc("奖励列表信息")
    public List<BuyOneGetSevenDetailInfo> activityData;
    @ProtoDesc("活动结束时间戳，毫秒")
    public long activityEndTime;
    @ProtoDesc("可购买截止时间戳，毫秒")
    public long buyEndTime;
    @ProtoDesc("售价")
    public String sellingPrice;
    @ProtoDesc("渠道商品ID")
    public String productId;
    @ProtoDesc("是否已购买")
    public boolean isBuy;
    @ProtoDesc("支付成功后直接到账的奖励；普通详情查询时为空")
    public List<ItemInfo> infoList;

    public ResBuyOneGetSeven(int code) {
        super(code);
    }
}
