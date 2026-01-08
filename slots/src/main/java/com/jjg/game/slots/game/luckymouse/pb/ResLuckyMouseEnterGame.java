package com.jjg.game.slots.game.luckymouse.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.luckymouse.LuckyMouseConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LUCKY_MOUSE, cmd = LuckyMouseConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("进入游戏，返回配置信息")
public class ResLuckyMouseEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("当前奖池的值")
    public long poolValue;
    @ProtoDesc("状态  0.普通   1.真免费   2.假免费")
    public int status;

    public ResLuckyMouseEnterGame(int code) {
        super(code);
    }
}
