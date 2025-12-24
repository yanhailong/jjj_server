package com.jjg.game.slots.game.moneyrabbit.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.moneyrabbit.MoneyRabbitConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MONEY_RABBIT, cmd = MoneyRabbitConstant.MsgBean.RES_ENTER_GAME, resp = true)
@ProtoDesc("返回配置信息")
public class ResMoneyRabbitEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("当前奖池的值")
    public long poolValue;
    @ProtoDesc("状态  0.普通   1.真免费   2.假免费")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("奖池配置信息")
    public List<MoneyRabbitPoolInfo> poolList;
    @ProtoDesc("免费模式累计奖励")
    public long freeModeTotalReward;

    public ResMoneyRabbitEnterGame(int code) {
        super(code);
    }
}
