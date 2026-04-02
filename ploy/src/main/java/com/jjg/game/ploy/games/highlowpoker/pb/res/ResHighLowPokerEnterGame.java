package com.jjg.game.ploy.games.highlowpoker.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerConstant;
import com.jjg.game.ploy.games.highlowpoker.pb.bean.HighLowHistoryInfo;

import java.util.List;

/**
 * @author lm
 * @date 2026/4/1 09:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HIGH_LOW_POKER, cmd = HighLowPokerConstant.MsgBean.RES_HIGH_LOW_POKER_ENTER_GAME, resp = true)
@ProtoDesc("进入游戏")
public class ResHighLowPokerEnterGame extends AbstractResponse {
    @ProtoDesc("剩余牌数")
    public int remainCardNum;
    @ProtoDesc("下注区域赔率 从红色系开始然后黑色系最后大于小于")
    public List<String> chooseRate;
    @ProtoDesc("历史选择 key牌id，value赔率")
    public List<HighLowHistoryInfo> historyChoose;
    @ProtoDesc("当前牌")
    public int currentCard;
    @ProtoDesc("当前能兑现的金币数量")
    public long currentCoin;
    @ProtoDesc("押注列表")
    public List<Integer> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;

    public ResHighLowPokerEnterGame(int code) {
        super(code);
    }
}
