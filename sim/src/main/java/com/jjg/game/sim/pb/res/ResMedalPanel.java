package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.MedalQualityInfo;

import java.util.List;

/**
 * 成就勋章面板返回 (成就-勋章子页)。
 * 勋章静态信息(名称/描述/品质底框)客户端依配置自取, 服务端只下发达成/排行/品质统计与加成档。
 *
 * @author 11
 * @date 2026/7/2
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_MEDAL_PANEL, resp = true)
@ProtoDesc("成就勋章面板返回")
public class ResMedalPanel extends AbstractResponse {
    @ProtoDesc("已激活勋章配置id")
    public List<Integer> activatedMedalIds;
    @ProtoDesc("勋章总数 (配置中已开启的勋章数量)")
    public int totalMedalCount;
    @ProtoDesc("全服排名超过百分比 (万分比: 9000=90.00%)")
    public int rankPermil;
    @ProtoDesc("各品质统计与加成档")
    public List<MedalQualityInfo> qualityInfos;
    @ProtoDesc("勋章最大展示数量")
    public int medalShowMax;

    public ResMedalPanel(int code) {
        super(code);
    }
}
