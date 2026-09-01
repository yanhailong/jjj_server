package com.jjg.game.hall.minigame.game.mining.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.hall.minigame.game.mining.MiningConstant;
import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.RES_RANK, resp = true)
@ProtoDesc("挖矿排行榜")
public class ResMiningRank extends AbstractResponse {
    @ProtoDesc("当前赛季ID")
    public String seasonId;
    @ProtoDesc("当前存档版本")
    public long version;
    @ProtoDesc("前300名")
    public List<MiningRankInfo> ranks;
    @ProtoDesc("自己排名，超过300也返回")
    public MiningRankInfo self;
    public ResMiningRank(int code) { super(code); }
}
