package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.hulk.HulkConstant;

/**
 * @author 11
 * @date 2026/4/23
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HULK, cmd = HulkConstant.MsgBean.RES_MINI_CAR, resp = true)
@ProtoDesc("汽车小游戏返回")
public class ResHulkMiniGameCar extends AbstractResponse {
    @ProtoDesc("中奖金额")
    public long gold;
    @ProtoDesc("是否进入飞机小游戏")
    public boolean toAirPlane;

    public ResHulkMiniGameCar(int code) {
        super(code);
    }
}
