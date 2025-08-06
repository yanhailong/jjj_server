package com.jjg.game.poker.game.texas.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.common.message.bean.PlayerInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/5 19:58
 */
@ProtobufMessage
@ProtoDesc("德州玩家信息")
public class TexasPlayerInfo {
    @ProtoDesc("基本玩家信息")
    public PlayerInfo playerInfo;
    @ProtoDesc("总押注")
    public long totalBet;
    @ProtoDesc("当前手牌")
    public List<Integer> handCards;
}
