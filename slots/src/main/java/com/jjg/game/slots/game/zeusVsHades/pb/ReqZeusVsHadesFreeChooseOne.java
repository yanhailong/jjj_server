package com.jjg.game.slots.game.zeusVsHades.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.zeusVsHades.ZeusVsHadesConstant;

/**
 * @author lihaocao
 * @date 2025/12/1 18:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ZEUS_VS_HADES, cmd = ZeusVsHadesConstant.MsgBean.REQ_FREE_CHOOSE_ONE)
@ProtoDesc("请求免费模式二选一")
public class ReqZeusVsHadesFreeChooseOne extends AbstractMessage {
    @ProtoDesc("免费模式类型  0.宙斯  1.哈里斯")
    public int type;
}
