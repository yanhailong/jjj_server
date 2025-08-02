package com.jjg.game.poker.game.texas.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/31 14:26
 */
@ProtobufMessage
@ProtoDesc("德州扑克轮次信息")
public class TexasRoundInfo {
    @ProtoDesc("当前轮次")
    public int round;
    @ProtoDesc("增加的牌")
    public List<Integer> cards;
    @ProtoDesc("自己的牌型")
    public int handType;
}
