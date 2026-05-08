package com.jjg.game.ploy.games.luckypoker.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/19
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_LUCKY_POKER, cmd = LuckyPokerConstant.MsgBean.RES_LUCKY_POKER_ENTER_GAME, resp = true)
@ProtoDesc("进入游戏返回")
public class ResLuckyPokerEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Integer> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("第一阶段的牌id")
    public List<Integer> pokerIds;

    public ResLuckyPokerEnterGame(int code) {
        super(code);
    }
}
