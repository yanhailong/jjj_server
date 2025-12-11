package com.jjg.game.slots.game.captainjack.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MAHJIONG_WIN_TYPE, cmd = CaptainJackConstant.MsgBean.RES_CAPTAIN_JACK_ENTER_GAME, resp = true)
@ProtoDesc("返回配置信息")
public class ResCaptainJackEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费 2.探宝")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("当前免费倍率")
    public int freeMultiplier;
    @ProtoDesc("累计探宝触发的倍率")
    public int accumulationRate;
    public ResCaptainJackEnterGame(int code) {
        super(code);
    }
}
