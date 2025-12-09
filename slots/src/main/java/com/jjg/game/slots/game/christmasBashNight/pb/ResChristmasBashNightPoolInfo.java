package com.jjg.game.slots.game.christmasBashNight.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.christmasBashNight.ChristmasBashNightConstant;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/12/2 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CHRISTMAS_NIGHT_TYPE, cmd = ChristmasBashNightConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("返回奖池信息")
public class ResChristmasBashNightPoolInfo extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResChristmasBashNightPoolInfo(int code) {
        super(code);
    }
}
