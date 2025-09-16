package com.jjg.game.activity.sharepromote.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.sharepromote.message.bean.SharePromoteActivityInfo;
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
@ProtoDesc("响应推广分享活动类型信息")
public class ResSharePromoteTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<SharePromoteActivityInfo> activityData;

    public ResSharePromoteTypeInfo(int code) {
        super(code);
    }
}
