package com.jjg.game.slots.game.bountyduel.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.bountyduel.BountyDuelConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BOUNTY_DUEL_TYPE, cmd = BountyDuelConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("返回赏金大对决配置")
public class ResBountyDuelEnterGame extends AbstractResponse {
    @ProtoDesc("下注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认下注")
    public long defaultBet;
    @ProtoDesc("连续中奖倍数信息")
    public List<BountyDuelAddTimesInfo> timesInfoList;
    @ProtoDesc("免费模式累计赢分")
    public long totalWinGold;
    @ProtoDesc("当前状态")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;

    public ResBountyDuelEnterGame(int code) {
        super(code);
    }
}
