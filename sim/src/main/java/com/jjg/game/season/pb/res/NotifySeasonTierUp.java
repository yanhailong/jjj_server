package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.NOTIFY_SEASON_TIER_UP, resp = true)
@ProtoDesc("通知段位升级")
public class NotifySeasonTierUp extends AbstractResponse {
    @ProtoDesc("升级前段位ID")
    public int oldTierId;
    @ProtoDesc("升级后段位ID")
    public int newTierId;
    @ProtoDesc("累计获得的赛季币")
    public long totalEarnedCoin;
    @ProtoDesc("升级到下一段位所需的累计赛季币 (已是最高段位时为0)")
    public long nextTierNeedCoin;

    public NotifySeasonTierUp(int code) {
        super(code);
    }
}
