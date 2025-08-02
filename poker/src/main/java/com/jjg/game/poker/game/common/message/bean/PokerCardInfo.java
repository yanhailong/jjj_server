package com.jjg.game.poker.game.common.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/8/2 17:10
 */
@ProtobufMessage
@ProtoDesc("扑克类牌信息")
public class PokerCardInfo {
    @ProtoDesc("配置表id")
    public int id;
    @ProtoDesc("资源id")
    public int cardId;
}
