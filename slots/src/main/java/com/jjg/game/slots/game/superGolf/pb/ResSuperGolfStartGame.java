package com.jjg.game.slots.game.superGolf.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superGolf.SuperGolfConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_GOLF_TYPE, cmd = SuperGolfConstant.MsgBean.RES_START_GAME, resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResSuperGolfStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("本局中奖金币")
    public long allWinGold;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示  1.sweet 2.big 3.mega 4.epic 5.legendary")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("初始中奖图标信息")
    public SuperGolfIconInfo rewardIconInfo;
    @ProtoDesc("cascade 链（消除/补齐/神秘转换）")
    public List<SuperGolfCascade> addIconInfoList;
    @ProtoDesc("本局应用的最终乘倍值（来自盘面神秘符号；普通模式按本局，免费模式见 freeMultiplierAccum）")
    public int multiplier;
    @ProtoDesc("免费模式累计倍率（含本局）；不在免费模式为 0")
    public int freeMultiplierAccum;

    public ResSuperGolfStartGame(int code) {
        super(code);
    }
}
