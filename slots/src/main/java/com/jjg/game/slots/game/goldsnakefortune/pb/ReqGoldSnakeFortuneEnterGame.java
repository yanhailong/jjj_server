package com.jjg.game.slots.game.goldsnakefortune.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.goldsnakefortune.GoldSnakeFortuneConstant;

/**
 * @author 11
 * @date 2025/8/27 11:01
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.GOLD_SNAKE_FORTUNE, cmd = GoldSnakeFortuneConstant.MsgBean.REQ_ENTER_GAME)
@ProtoDesc("请求配置信息")
public class ReqGoldSnakeFortuneEnterGame extends AbstractMessage {
}
