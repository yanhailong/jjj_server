package com.jjg.game.sim.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.NOTIFY_SEASON_MATCH_RESULT, resp = true)
@ProtoDesc("通知赛季对局结算")
public class NotifySeasonMatchResult extends AbstractResponse {
    @ProtoDesc("对局ID")
    public String matchId;
    @ProtoDesc("结果：-1负，0平，1胜")
    public int result;
    @ProtoDesc("自己的总收益")
    public long playerTotalWin;
    @ProtoDesc("对手的总收益")
    public long opponentTotalWin;
    @ProtoDesc("赛季币变化")
    public long coinChange;
    @ProtoDesc("结算后的赛季币")
    public long seasonCoin;

    public NotifySeasonMatchResult(int code) {
        super(code);
    }
}
