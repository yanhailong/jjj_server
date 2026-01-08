package com.jjg.game.slots.game.elephantgod.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.elephantgod.ElephantGodConstant;
import com.jjg.game.slots.game.luckymouse.LuckyMouseConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ELEPHANT_GOD, cmd = ElephantGodConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("进入游戏，返回配置信息")
public class ResElephantGodEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("当前奖池的值")
    public long poolValue;
    @ProtoDesc("状态  0.普通   1.真免费   2.假免费")
    public int status;

    public ResElephantGodEnterGame(int code) {
        super(code);
    }
}
