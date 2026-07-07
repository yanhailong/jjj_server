package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 成就勋章按品质的统计与加成档信息。
 * 品质: 2=精英 3=大亨 4=富翁 5=神豪 (取自勋章道具 ItemCfg.quality)。
 *
 * @author 11
 * @date 2026/7/2
 */
@ProtobufMessage
@ProtoDesc("勋章品质统计与加成档")
public class MedalQualityInfo {
    @ProtoDesc("勋章品质 (2精英/3大亨/4富翁/5神豪)")
    public int quality;
    @ProtoDesc("该品质已激活勋章数量")
    public int collectedCount;
    @ProtoDesc("已激活的加成档配置id (MedalBuff.id, 0=未达任何档)")
    public int activatedBuffCfgId;
}
