package com.jjg.game.activity.scratchcards.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.scratchcards.message.bean.ScratchCardsActivity;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_SCRATCH_CARDS_TYPE_INFO,resp = true)
@ProtoDesc("响应刮刮乐类型信息")
public class ResScratchCardsTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<ScratchCardsActivity> activityData;

    public ResScratchCardsTypeInfo(int code) {
        super(code);
    }
}
