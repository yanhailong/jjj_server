package com.jjg.game.ploy.games.luckypoker.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/4/20
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_LUCKY_POKER, cmd = LuckyPokerConstant.MsgBean.RES_RECORD, resp = true)
@ProtoDesc("游戏记录返回")
public class ResLuckyPokerPloyRecord extends AbstractResponse {
    public List<LuckyPokerRecordInfo> records;

    public ResLuckyPokerPloyRecord(int code) {
        super(code);
    }
}
