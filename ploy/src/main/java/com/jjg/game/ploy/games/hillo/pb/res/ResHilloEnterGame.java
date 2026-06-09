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
@ProtoDesc("进入 HILLO")
public class ResHilloEnterGame extends AbstractResponse {
    @ProtoDesc("当前公牌")
    public int currentCard;
    @ProtoDesc("当前可兑现奖励")
    public long currentCoin;
    @ProtoDesc("本局剩余可猜次数")
    public int remainRoundNum;
    @ProtoDesc("本局剩余跳过次数")
    public int remainSkipTimes;
    @ProtoDesc("当前下注模式")
    public int currentBetMode;
    @ProtoDesc("是否正在自动投注")
    public boolean autoBetting;
    @ProtoDesc("自动投注是否无限局")
    public boolean autoInfiniteBet;
    @ProtoDesc("自动投注每局下注金额")
    public long autoBet;
    @ProtoDesc("自动投注单局猜测次数")
    public int autoGuessTimes;
    @ProtoDesc("剩余自动投注局数，0 配合 autoInfiniteBet 表示无限局")
    public int autoRemainBetTimes;
    @ProtoDesc("当前可选投注项")
    public List<HilloChooseInfo> chooseInfos;
    @ProtoDesc("当前局过程记录")
    public List<HilloHistoryInfo> historyChoose;
    @ProtoDesc("下注范围配置")
    public List<Integer> stakeList;
    @ProtoDesc("默认下注金额")
    public long defaultBet;

    public ResHilloEnterGame(int code) {
        super(code);
    }
}
