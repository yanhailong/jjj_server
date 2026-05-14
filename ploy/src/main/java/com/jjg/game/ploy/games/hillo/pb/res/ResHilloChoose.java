package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloChooseInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_CHOOSE, resp = true)
@ProtoDesc("HILLO choose result")
public class ResHilloChoose extends AbstractResponse {
    @ProtoDesc("next card id")
    public int nextCardId;
    @ProtoDesc("current coin")
    public long currentCoin;
    @ProtoDesc("exchange num")
    public long exchangeNum;
    @ProtoDesc("remain round num")
    public int remainRoundNum;
    @ProtoDesc("remain skip times")
    public int remainSkipTimes;
    @ProtoDesc("next choose infos")
    public List<HilloChooseInfo> chooseInfos;

    public ResHilloChoose(int code) {
        super(code);
    }
}
