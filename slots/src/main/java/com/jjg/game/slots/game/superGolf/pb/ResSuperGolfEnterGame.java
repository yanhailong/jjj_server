package com.jjg.game.slots.game.superGolf.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superGolf.SuperGolfConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_GOLF_TYPE, cmd = SuperGolfConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("返回配置信息")
public class ResSuperGolfEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("累计中奖金币（免费模式累积）")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("免费模式累计倍率（不在免费模式时为 0）")
    public int freeMultiplierAccum;

    public ResSuperGolfEnterGame(int code) {
        super(code);
    }
}
