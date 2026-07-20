package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.pb.struct.SeasonTierRewardInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_TIER_UP_REWARDS, resp = true)
@ProtoDesc("段位升级奖励列表 (仅进阶/循环赛季有内容)")
public class ResSeasonTierUpRewards extends AbstractResponse {
    @ProtoDesc("按段位从低到高排列; obtained=true 表示已达到该段位, 晋升奖励已发放")
    public List<SeasonTierRewardInfo> rewards;
    @ProtoDesc("当前段位配置ID, 0表示无段位")
    public int tierId;

    public ResSeasonTierUpRewards(int code) {
        super(code);
    }
}
