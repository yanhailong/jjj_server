package com.jjg.game.activity.firstpayment.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:28
 */
@ProtobufMessage
@ProtoDesc("首充活动信息")
public class FirstPaymentActivityInfo {
    @ProtoDesc("活动详细信息")
    public List<FirstPaymentDetailInfo> detailInfos;
}
