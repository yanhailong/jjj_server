package com.jjg.game.poker.game.douxian.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 单个玩家单个区域的摆牌展示信息（出牌提示/出牌结果/结算亮牌均复用）
 */
@ProtobufMessage
@ProtoDesc("斗仙牌区域摆牌信息")
public class DouXianZonePlacementInfo {
    @ProtoDesc("区域(1凡界 2灵界 3仙界)")
    public int zoneId;
    @ProtoDesc("该区域内已公开的牌(客户端牌id)。本人视角=全部牌；他人视角在本回合结算亮牌前，只包含上回合飞升带来的已公开牌")
    public List<Integer> cardIds;
    @ProtoDesc("他人视角下，本回合尚未亮牌的隐藏牌数量(仅用于显示牌背张数)；本人视角恒为0")
    public int hiddenCount;
    @ProtoDesc("牌型名称(仅本人视角、或该回合已结算亮牌后才下发，避免结算前泄露对手牌型)")
    public String handTypeName;
    @ProtoDesc("灵力值(下发规则同handTypeName)")
    public long aetherValue;
    @ProtoDesc("是否锁定(飞升而来，本回合不可取回)")
    public boolean locked;
    @ProtoDesc("牌型多语言id(来自ImmortalHand.xlsx)")
    public int handTypeNameId;
}
