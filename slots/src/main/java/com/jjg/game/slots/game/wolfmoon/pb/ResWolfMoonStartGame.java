package com.jjg.game.slots.game.wolfmoon.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WOLF_MOON, cmd = WolfMoonConstant.MsgBean.RES_START_GAME, resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResWolfMoonStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("本局中奖金币")
    public long allWinGold;
    @ProtoDesc("免费累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示 1.sweet 2.big 3.mega 4.epic 5.legendary")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("中奖图标信息")
    public WolfMoonIconInfo rewardIconInfo;
    @ProtoDesc("消除补齐信息")
    public List<WolfMoonCascade> addIconInfoList;
    @ProtoDesc("倍数免费本局倍率")
    public int freeMultiple;

    public ResWolfMoonStartGame(int code) {
        super(code);
    }
}
