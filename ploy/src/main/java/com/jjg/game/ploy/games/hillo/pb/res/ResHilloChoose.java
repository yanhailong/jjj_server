package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.data.HilloHistoryInfo;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloChooseInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_CHOOSE, resp = true)
@ProtoDesc("HILLO 猜牌结果")
public class ResHilloChoose extends AbstractResponse {
    @ProtoDesc("本次发出的下一张牌 id")
    public int nextCardId;
    @ProtoDesc("当前可兑现奖励")
    public long currentCoin;
    @ProtoDesc("本次兑现到账金币")
    public long exchangeNum;
    @ProtoDesc("本局剩余可猜次数")
    public int remainRoundNum;
    @ProtoDesc("本局剩余跳过次数")
    public int remainSkipTimes;
    @ProtoDesc("下一轮可选投注项")
    public List<HilloChooseInfo> chooseInfos;
    @ProtoDesc("是否正在自动投注")
    public boolean autoBetting;
    @ProtoDesc("自动投注是否无限局")
    public boolean autoInfiniteBet;
    @ProtoDesc("本次动作，手动和自动都使用：0=无，1=开始新局，2=猜中，3=跳过，4=兑现，5=猜错，6=停止")
    public int action;
    @ProtoDesc("当前公牌 id")
    public int currentCard;
    @ProtoDesc("本次选择的投注项 id")
    public int chooseId;
    @ProtoDesc("剩余自动投注局数，0 配合 autoInfiniteBet 表示无限局")
    public int remainBetTimes;
    @ProtoDesc("当前局过程记录")
    public List<HilloHistoryInfo> historyChoose;

    public ResHilloChoose(int code) {
        super(code);
    }
}
