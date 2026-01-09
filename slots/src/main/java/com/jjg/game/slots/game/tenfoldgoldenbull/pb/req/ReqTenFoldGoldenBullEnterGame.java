package com.jjg.game.slots.game.tenfoldgoldenbull.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.tenfoldgoldenbull.constant.TenFoldGoldenBullConstant;

/**
 * @author 11
 * @date 2025/8/1 17:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TENFOLD_GOLDEN_BULL, cmd = TenFoldGoldenBullConstant.MsgBean.REQ_TEN_FOLD_GOLDEN_BULL_ENTER_GAME)
@ProtoDesc("请求配置信息")
public class ReqTenFoldGoldenBullEnterGame extends AbstractMessage {
}
