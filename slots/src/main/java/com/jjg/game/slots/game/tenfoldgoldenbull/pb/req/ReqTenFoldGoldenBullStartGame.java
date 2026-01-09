package com.jjg.game.slots.game.tenfoldgoldenbull.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.tenfoldgoldenbull.constant.TenFoldGoldenBullConstant;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = TenFoldGoldenBullConstant.MsgBean.REQ_TEN_FOLD_GOLDEN_BULL_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqTenFoldGoldenBullStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
