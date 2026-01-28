package com.jjg.game.slots.game.zeusVsHades.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.zeusVsHades.ZeusVsHadesConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ZEUS_VS_HADES, cmd = ZeusVsHadesConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqZeusVsHadesStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
