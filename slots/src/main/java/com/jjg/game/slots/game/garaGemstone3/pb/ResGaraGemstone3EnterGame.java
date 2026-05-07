package com.jjg.game.slots.game.garaGemstone3.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.garaGemstone3.GaraGemstone3Constant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.GARA_GEMSTONE_3, cmd = GaraGemstone3Constant.MsgBean.RES_GARA_GEMSTONE_3_CONFIG_INFO, resp = true)
@ProtoDesc("进入游戏，返回配置信息")
public class ResGaraGemstone3EnterGame extends AbstractResponse {
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
    public List<GaraGemstone3PoolInfo> poolList;

    public ResGaraGemstone3EnterGame(int code) {
        super(code);
    }
}
