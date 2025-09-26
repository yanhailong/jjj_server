package com.jjg.game.activity.officialawards.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.officialawards.message.bean.OfficialAwardsActivity;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_OFFICIAL_AWARDS_DETAIL_INFO,resp = true)
@ProtoDesc("官方派奖类型信息")
public class ResOfficialAwardsTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<OfficialAwardsActivity> activityData;

    public ResOfficialAwardsTypeInfo(int code) {
        super(code);
    }
}
