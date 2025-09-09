package com.jjg.game.activity.privilegecard.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.privilegecard.message.bean.PrivilegeCardType;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_PRIVILEGE_CARD_TYPE_INFO,resp = true)
@ProtoDesc("响应每日奖金活动类型信息")
public class ResPrivilegeCardTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<PrivilegeCardType> activityData;

    public ResPrivilegeCardTypeInfo(int code) {
        super(code);
    }
}
