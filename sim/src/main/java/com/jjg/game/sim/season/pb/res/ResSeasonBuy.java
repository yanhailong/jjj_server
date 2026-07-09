package com.jjg.game.sim.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import java.util.Map;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SEASON_BUY, resp = true)
@ProtoDesc("购买赛季商品返回")
public class ResSeasonBuy extends AbstractResponse {
    @ProtoDesc("获得道具")
    public Map<Integer, Long> goods;
    @ProtoDesc("购买后的赛季币")
    public long seasonCoin;
    @ProtoDesc("该商品当前周期已购买次数")
    public int purchased;

    public ResSeasonBuy(int code) {
        super(code);
    }
}
