package com.jjg.game.activity.wealthroulette.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteGoodInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/12/1 10:15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_WEALTH_ROULETTE_BUY_GOOD,resp = true)
@ProtoDesc("财富轮盘购买商品结果")
public class ResWealthRouletteBuyGood extends AbstractResponse {
    @ProtoDesc("购买数量")
    public int bugNum;
    @ProtoDesc("商品信息")
    public WealthRouletteGoodInfo goodInfo;
    @ProtoDesc("剩余积分")
    public int remainPoint;

    public ResWealthRouletteBuyGood(int code) {
        super(code);
    }
}
