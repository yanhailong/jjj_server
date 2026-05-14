package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloChooseInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_SKIP, resp = true)
@ProtoDesc("HILLO skip result")
public class ResHilloSkip extends AbstractResponse {
    @ProtoDesc("current card id")
    public int currentCard;
    @ProtoDesc("remain skip times")
    public int remainSkipTimes;
    @ProtoDesc("choose infos")
    public List<HilloChooseInfo> chooseInfos;

    public ResHilloSkip(int code) {
        super(code);
    }
}
