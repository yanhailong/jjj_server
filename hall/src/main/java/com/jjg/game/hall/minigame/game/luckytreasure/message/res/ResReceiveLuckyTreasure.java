package com.jjg.game.hall.minigame.game.luckytreasure.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.LuckyTreasureConstant;

/**
 * 请求领取夺宝奇兵道具
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = LuckyTreasureConstant.Message.RES_RECEIVE_LUCKY_TREASURE,
        resp = true
)
@ProtoDesc("请求领取夺宝奇兵道具回返")
public class ResReceiveLuckyTreasure extends AbstractResponse {

    public ResReceiveLuckyTreasure(int code) {
        super(code);
    }

}
