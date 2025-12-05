package com.jjg.game.activity.wealthroulette.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteDrawInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/1 10:15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_WEALTH_ROULETTE_DRAW, resp = true)
@ProtoDesc("财富轮盘抽取结果")
public class ResWealthRouletteDraw extends AbstractResponse {
    @ProtoDesc("抽奖结果")
    public List<WealthRouletteDrawInfo> drawInfos;
    @ProtoDesc("剩余积分")
    public long remainPoint;

    public ResWealthRouletteDraw(int code) {
        super(code);
    }
}
