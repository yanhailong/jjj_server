package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;

/**
 * @author lm
 * @date 2025/7/29 13:36
 */

@ProtobufMessage
@ProtoDesc("响应房间基本信息")
public class RepsBlackJackRoomBaseInfo extends AbstractResponse {
    public RepsBlackJackRoomBaseInfo(int code) {
        super(code);
    }
}
