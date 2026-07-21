package com.jjg.game.poker.game.douxian.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 结算时某个玩家全部区域的亮牌快照，DESIGN.md 8.9：结算已经发生，此时展示所有玩家的牌面
 * 不存在偷看问题(区别于摆牌阶段的隐藏规则，见DESIGN.md 8.8)。
 */
@ProtobufMessage
@ProtoDesc("斗仙牌结算亮牌信息")
public class DouXianRevealInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("该玩家本回合三个区域的完整摆牌(含牌面/牌型名称/灵力值)，结算已发生，对所有人可见")
    public List<DouXianZonePlacementInfo> zones;
}
