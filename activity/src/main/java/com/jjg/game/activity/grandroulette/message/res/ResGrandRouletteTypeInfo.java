package com.jjg.game.activity.grandroulette.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.grandroulette.message.bean.GrandRouletteActivityInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_GRAND_ROULETTE_TYPE_INFO,resp = true)
@ProtoDesc("大转盘活动类型信息")
public class ResGrandRouletteTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<GrandRouletteActivityInfo> activityData;

    public ResGrandRouletteTypeInfo(int code) {
        super(code);
    }
}
