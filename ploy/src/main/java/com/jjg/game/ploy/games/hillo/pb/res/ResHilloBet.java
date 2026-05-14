package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloChooseInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_BET, resp = true)
@ProtoDesc("HILLO bet")
public class ResHilloBet extends AbstractResponse {
    @ProtoDesc("current card")
    public int currentCard;
    @ProtoDesc("remain round num")
    public int remainRoundNum;
    @ProtoDesc("remain skip times")
    public int remainSkipTimes;
    @ProtoDesc("choose infos")
    public List<HilloChooseInfo> chooseInfos;

    public ResHilloBet(int code) {
        super(code);
    }
}
