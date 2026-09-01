package com.jjg.game.hall.minigame.game.mining.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.game.mining.MiningConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.RES_ACHIEVEMENTS, resp = true)
@ProtoDesc("挖矿成就")
public class ResMiningAchievements extends AbstractResponse {
    @ProtoDesc("当前赛季ID")
    public String seasonId;
    @ProtoDesc("当前存档版本")
    public long version;
    @ProtoDesc("成就：可领、未完成、已领取排序")
    public List<MiningTaskInfo> achievements;

    public ResMiningAchievements(int code) { super(code); }
}
