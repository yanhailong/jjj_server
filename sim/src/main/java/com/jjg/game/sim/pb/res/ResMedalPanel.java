package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.AchievementBadgeInfo;

import java.util.List;

/**
 * 成就徽章面板返回。静态名称、图标与档位展示由客户端配置提供，服务端下发权威进度、档位和加成。
 *
 * @author 11
 * @date 2026/7/2
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_MEDAL_PANEL, resp = true)
@ProtoDesc("成就勋章面板返回")
public class ResMedalPanel extends AbstractResponse {
    @ProtoDesc("已完成成就任务数")
    public int completedAchievementCount;
    @ProtoDesc("成就任务总数")
    public int totalAchievementCount;
    @ProtoDesc("全服排名超过百分比 (万分比: 9000=90.00%)")
    public int rankBasisPoints;
    @ProtoDesc("所有已激活徽章档位的固定值加成汇总，key=BuildingOutputType.code")
    public List<KVInfo> activeBuffs;
    @ProtoDesc("全部成就徽章进度，已解锁且档位高的排在前面")
    public List<AchievementBadgeInfo> badgeInfos;
    @ProtoDesc("勋章最大展示数量")
    public int medalShowMax;
    @ProtoDesc("展示中的徽章ID(MedalBuff.MedalType)，顺序即展示顺序")
    public List<Integer> showMedalIds;

    public ResMedalPanel(int code) {
        super(code);
    }
}
