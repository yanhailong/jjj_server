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
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HULK, cmd = HulkConstant.MsgBean.RES_MINI_AIRPLANE, resp = true)
@ProtoDesc("飞机小游戏返回")
public class ResHulkMiniGameAirPlane extends AbstractResponse {
    @ProtoDesc("中奖倍数")
    public int times;
    @ProtoDesc("中奖金币")
    public long allWinGold;
    @ProtoDesc("玩家当前金币")
    public long allGold;

    public ResHulkMiniGameAirPlane(int code) {
        super(code);
    }
}
