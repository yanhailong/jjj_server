package com.jjg.game.slots.game.captainjack.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;

/**
 * @author 11
 * @date 2025/8/1 17:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CAPTAIN_JACK, cmd = CaptainJackConstant.MsgBean.RES_CAPTAIN_JACK_TREASURE_HUNTING, resp = true)
@ProtoDesc("返回探宝信息")
public class ResCaptainJackTreasureHunting extends AbstractResponse {
    @ProtoDesc("本次探宝触发的倍率")
    public int currentRate;
    @ProtoDesc("剩余探宝次数")
    public int remainDigCount;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;

    public ResCaptainJackTreasureHunting(int code) {
        super(code);
    }
}
