package com.jjg.game.slots.game.elephantgod.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.captainjack.pb.bean.CaptainJackWinIconInfo;
import com.jjg.game.slots.game.elephantgod.ElephantGodConstant;
import com.jjg.game.slots.game.elephantgod.pb.bean.ElephantGodWinIconInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ELEPHANT_GOD, cmd = ElephantGodConstant.MsgBean.RES_START_GAME, resp = true)
@ProtoDesc("返回游戏信息")
public class ResElephantGodStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("当前状态 0.正常  1.免费 2.探宝")
    public int status;
    @ProtoDesc("累计中奖金币")
    public long allWinGold;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示  1.sweet   2.big   3.mega  4.epic  5.legendary")
    public int bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("中奖图标信息")
    public ElephantGodWinIconInfo rewardIconInfo;
    public ResElephantGodStartGame(int code) {
        super(code);
    }
}
