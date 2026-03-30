package com.jjg.game.slots.game.angrybirds.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.angrybirds.constant.AngryBirdsConstant;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ANGRY_BIRDS, cmd = AngryBirdsConstant.MsgBean.REQ_ANGRY_BIRDS_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqAngryBirdsStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
