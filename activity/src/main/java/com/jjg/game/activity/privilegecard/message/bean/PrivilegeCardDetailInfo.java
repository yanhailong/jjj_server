package com.jjg.game.activity.privilegecard.message.bean;

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
@ProtoDesc("每日奖金")
public class PrivilegeCardDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("总获利")
    public List<ItemInfo> totalGet;
    @ProtoDesc("已领取总金币")
    public ItemInfo hasClaimNum;
    @ProtoDesc("需要充值金额")
    public int rechargePrice;
    @ProtoDesc("剩余时间 毫秒")
    public long remainTime;
}
