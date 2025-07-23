package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/20 10:18
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = SlotsConst.MsgBean.RES_INVEST_AREA,resp = true)
@ProtoDesc("选择投资地区返回")
public class ResInvestArea extends AbstractResponse {
    @ProtoDesc("回报金额列表")
    public List<Long> goldList;
    @ProtoDesc("3次都中奖，奖励的火车信息")
    public TrainInfo allWinTrainInfo;
    @ProtoDesc("是否全地图解锁")
    public boolean allAreaUnLock;

    public ResInvestArea(int code) {
        super(code);
    }
}
