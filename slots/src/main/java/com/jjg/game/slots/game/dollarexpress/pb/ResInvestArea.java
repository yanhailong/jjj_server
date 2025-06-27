package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/20 10:18
 */
@ProtobufMessage(messageType = DollarExpressConst.MsgBean.TYPE, cmd = DollarExpressConst.MsgBean.RES_INVEST_AREA,resp = true)
@ProtoDesc("选择投资地区返回")
public class ResInvestArea extends AbstractResponse {
    @ProtoDesc("回报金额1")
    public long areaGold1;
    @ProtoDesc("回报金额2")
    public long areaGold2;
    @ProtoDesc("回报金额3")
    public long areaGold3;
    @ProtoDesc("火车信息")
    public List<TrainInfo> trainInfoList;

    public ResInvestArea(int code) {
        super(code);
    }
}
