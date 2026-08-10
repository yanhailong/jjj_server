package com.jjg.game.poker.game.douxian.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * Recommended arrangement for one DouXian zone. Visible only to the owning player.
 */
@ProtobufMessage
@ProtoDesc("DouXian zone recommendation")
public class DouXianRecommendZoneInfo {
    @ProtoDesc("Zone: 1=MORTAL, 2=SPIRIT, 3=IMMORTAL")
    public int zoneId;
    @ProtoDesc("Recommended adjustable cards for this zone (client card ids, excludes locked cards)")
    public List<Integer> cardIds;
    @ProtoDesc("Carried and locked cards in this zone (client card ids)")
    public List<Integer> lockedCardIds;
    @ProtoDesc("Recommended hand type display name")
    public String handTypeName;
    @ProtoDesc("Recommended hand type multilingual id from ImmortalHand.xlsx")
    public int handTypeNameId;
    @ProtoDesc("Recommended hand aether value")
    public long aetherValue;
}
