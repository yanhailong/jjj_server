package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("百家乐结算信息")
public class BaccaratSettlementInfo {

    @ProtoDesc("庄家牌ID信息")
    public List<Byte> bankerCardIds;

    @ProtoDesc("庄家补牌ID")
    public byte extraBankerCardId;

    @ProtoDesc("庄家总点数")
    public byte bankerPointId;

    @ProtoDesc("闲家牌ID信息")
    public List<Byte> playerCardIds;

    @ProtoDesc("闲家补牌ID")
    public byte extraPlayerCardId;

    @ProtoDesc("闲家总点数")
    public byte playerPointId;

    @ProtoDesc("牌型状态")
    public BaccaratCardState cardState;
}
