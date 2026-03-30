package com.jjg.game.slots.game.elephantgod.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.elephantgod.ElephantGodConstant;
import com.jjg.game.slots.game.elephantgod.pb.bean.ElephantGodPoolInfo;

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
    @ProtoDesc("状态  0.普通   1.免费  ")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("当前倍数")
    public int currentMultiplier;
    @ProtoDesc("当前wild图标数量")
    public int wildCount;
    @ProtoDesc("免费模式累计金额")
    public long freeTotalWinGold;
    @ProtoDesc("奖池信息")
    public List<ElephantGodPoolInfo> poolList;
    public ResElephantGodEnterGame(int code) {
        super(code);
    }
}
