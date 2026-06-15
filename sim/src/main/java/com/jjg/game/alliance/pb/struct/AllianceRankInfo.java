package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 联盟排行榜项 (全服声誉榜)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("联盟排行项")
public class AllianceRankInfo {
    @ProtoDesc("排名(1起)")
    public int rank;
    @ProtoDesc("联盟概要")
    public AllianceBrief alliance;
    @ProtoDesc("榜单分值(声誉值)")
    public long score;
}
