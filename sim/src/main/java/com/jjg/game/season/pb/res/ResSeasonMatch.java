package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_MATCH, resp = true)
@ProtoDesc("赛季匹配返回")
public class ResSeasonMatch extends AbstractResponse {
    @ProtoDesc("对局ID")
    public String matchId;
    @ProtoDesc("对手玩家ID")
    public long opponentId;
    @ProtoDesc("对手玩家昵称")
    public String opponentName;
    @ProtoDesc("对手头像id")
    public int opponentHeadImgId;
    @ProtoDesc("对手头像框id")
    public int opponentHeadFrameId;
    @ProtoDesc("游戏ID")
    public int gameType;
    @ProtoDesc("下注赛季币")
    public long stake;
    @ProtoDesc("需要完成的旋转次数")
    public int expectedSpins;
    @ProtoDesc("对手代表战绩逐局收益")
    public List<Long> opponentSpinWins;
    @ProtoDesc("匹配后的赛季币")
    public long seasonCoin;

    public ResSeasonMatch() {
        super(0);
    }

    public ResSeasonMatch(int code) {
        super(code);
    }
}
