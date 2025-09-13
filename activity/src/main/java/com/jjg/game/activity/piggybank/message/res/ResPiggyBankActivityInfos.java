package com.jjg.game.activity.piggybank.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.piggybank.message.bean.PiggyBankActivityInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_PIGGY_BANK_ACTIVITY_INFOS,resp = true)
@ProtoDesc("响应每日奖金活动类型信息")
public class ResPiggyBankActivityInfos extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<PiggyBankActivityInfo> activityData;

    public ResPiggyBankActivityInfos(int code) {
        super(code);
    }
}
