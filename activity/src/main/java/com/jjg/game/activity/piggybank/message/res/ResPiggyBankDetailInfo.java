package com.jjg.game.activity.piggybank.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.piggybank.message.bean.PiggyBankDetailInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 17:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_PIGGY_BANK_CLAIM_REWARDS, resp = true)
@ProtoDesc("响应储钱罐活动详细信息")
public class ResPiggyBankDetailInfo extends AbstractResponse {
    @ProtoDesc("活动详细信息")
    public List<PiggyBankDetailInfo> detailInfo;

    public ResPiggyBankDetailInfo(int code) {
        super(code);
    }
}
