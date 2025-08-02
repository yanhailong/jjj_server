package com.jjg.game.poker.game.texas.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/2 16:30
 */
@ProtobufMessage
@ProtoDesc("池奖励信息")
public class TexasPotInfo {
    @ProtoDesc("玩家id信息")
    public List<Long> playerIdPotInfos;
}
