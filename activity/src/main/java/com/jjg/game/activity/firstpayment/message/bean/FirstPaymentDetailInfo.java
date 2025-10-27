package com.jjg.game.activity.firstpayment.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("首充详细信息")
public class FirstPaymentDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("购买奖励")
    public List<ItemInfo> bugGet;
    @ProtoDesc("需要充值金额")
    public String rechargePrice;
    @ProtoDesc("原价值")
    public String wasPrice;
    @ProtoDesc("超值")
    public String bestValue;
    @ProtoDesc("商品id")
    public String productId;
}
