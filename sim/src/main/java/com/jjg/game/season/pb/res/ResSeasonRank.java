package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.pb.struct.SeasonRankInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_RANK, resp = true)
@ProtoDesc("当前赛季排行榜返回")
public class ResSeasonRank extends AbstractResponse {
    @ProtoDesc("排行榜条目")
    public List<SeasonRankInfo> entries;
    @ProtoDesc("自己的排行榜信息")
    public SeasonRankInfo selfRankInfo;
    @ProtoDesc("赛季结束时间戳，毫秒")
    public long endTime;

    public ResSeasonRank(int code) {
        super(code);
    }
}
