package com.jjg.game.table.redblackwar.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/7/11 17:21
 */
@ProtobufMessage
@ProtoDesc("红黑大战历史记录")
public class RedBlackWarHistory {
    @ProtoDesc("获胜方(1红 2黑)")
    public int winner;
    @ProtoDesc("牌型 ")
    public int cardType;
}
