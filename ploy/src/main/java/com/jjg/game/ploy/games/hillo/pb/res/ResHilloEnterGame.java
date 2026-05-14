package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.data.HilloHistoryInfo;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloChooseInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_ENTER_GAME, resp = true)
@ProtoDesc("enter HILLO")
public class ResHilloEnterGame extends AbstractResponse {
    @ProtoDesc("current card")
    public int currentCard;
    @ProtoDesc("current coin")
    public long currentCoin;
    @ProtoDesc("remain round num")
    public int remainRoundNum;
    @ProtoDesc("remain skip times")
    public int remainSkipTimes;
    @ProtoDesc("current bet mode")
    public int currentBetMode;
    @ProtoDesc("auto betting")
    public boolean autoBetting;
    @ProtoDesc("auto bet times is infinite")
    public boolean autoInfiniteBet;
    @ProtoDesc("auto bet amount")
    public long autoBet;
    @ProtoDesc("auto guess times")
    public int autoGuessTimes;
    @ProtoDesc("remain auto bet times, 0 means infinite")
    public int autoRemainBetTimes;
    @ProtoDesc("choose infos")
    public List<HilloChooseInfo> chooseInfos;
    @ProtoDesc("history choose")
    public List<HilloHistoryInfo> historyChoose;
    @ProtoDesc("stake list")
    public List<Integer> stakeList;
    @ProtoDesc("default bet")
    public long defaultBet;

    public ResHilloEnterGame(int code) {
        super(code);
    }
}
