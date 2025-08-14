package com.jjg.game.room.message.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 申请上庄玩家信息
 *
 * @author Administrator
 */
@ProtoDesc("申请上庄玩家信息")
@ProtobufMessage
public class ApplyBankPlayerInfo {

    @ProtoDesc("玩家信息")
    public BasePlayerInfo basePlayerInfo;

    @ProtoDesc("准备金数量")
    public long predictCostGold;
}
