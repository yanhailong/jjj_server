package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.data.HilloHistoryInfo;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloChooseInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_AUTO_BET, resp = true)
@ProtoDesc("HILLO auto bet result")
public class ResHilloAutoBet extends AbstractResponse {
    @ProtoDesc("auto betting")
    public boolean autoBetting;
    @ProtoDesc("auto bet times is infinite")
    public boolean autoInfiniteBet;
    @ProtoDesc("auto action")
    public int action;
    @ProtoDesc("current card id")
    public int currentCard;
    @ProtoDesc("next card id")
    public int nextCardId;
    @ProtoDesc("choose id")
    public int chooseId;
    @ProtoDesc("current coin")
    public long currentCoin;
    @ProtoDesc("exchange num")
    public long exchangeNum;
    @ProtoDesc("remain round num")
    public int remainRoundNum;
    @ProtoDesc("remain skip times")
    public int remainSkipTimes;
    @ProtoDesc("remain auto bet times, 0 means infinite")
    public int remainBetTimes;
    @ProtoDesc("choose infos")
    public List<HilloChooseInfo> chooseInfos;
    @ProtoDesc("history choose")
    public List<HilloHistoryInfo> historyChoose;

    public ResHilloAutoBet(int code) {
        super(code);
    }
}
