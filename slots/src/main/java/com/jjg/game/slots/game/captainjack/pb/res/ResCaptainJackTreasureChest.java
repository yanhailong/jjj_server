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
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CAPTAIN_JACK, cmd = CaptainJackConstant.MsgBean.RES_CAPTAIN_JACK_TREASURE_CHEST)
@ProtoDesc("探宝")
public class ResCaptainJackTreasureChest extends AbstractResponse {
    @ProtoDesc("本次探宝触发的倍率")
    public int rate;
    @ProtoDesc("结算金额 有值的时候为结算")
    public long amount;
    public ResCaptainJackTreasureChest(int code) {
        super(code);
    }
}
