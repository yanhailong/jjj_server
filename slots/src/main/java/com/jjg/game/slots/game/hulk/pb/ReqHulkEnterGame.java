package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.goldsnakefortune.GoldSnakeFortuneConstant;
import com.jjg.game.slots.game.hulk.HulkConstant;

/**
 * @author 11
 * @date 2025/8/27 11:01
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HULK, cmd = HulkConstant.MsgBean.REQ_ENTER_GAME)
@ProtoDesc("请求配置信息")
public class ReqHulkEnterGame extends AbstractMessage {
}
