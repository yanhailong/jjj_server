package com.jjg.game.slots.game.zeusVsHades.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.zeusVsHades.ZeusVsHadesConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ZEUS_VS_HADES, cmd = ZeusVsHadesConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("返回奖池信息")
public class ResZeusVsHadesPoolInfo extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResZeusVsHadesPoolInfo(int code) {
        super(code);
    }
}
