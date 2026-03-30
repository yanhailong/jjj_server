package com.jjg.game.slots.game.tigerbringsriches.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.tigerbringsriches.constant.TigerBringsRichesConstant;

/**
 * @author 11
 * @date 2025/8/1 17:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = TigerBringsRichesConstant.MsgBean.REQ_TIGER_BRINGS_RICHES_ENTER_GAME)
@ProtoDesc("请求配置信息")
public class ReqTigerBringsRichesEnterGame extends AbstractMessage {
}
