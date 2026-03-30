package com.jjg.game.slots.game.goldsnakefortune.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.goldsnakefortune.GoldSnakeFortuneConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.GOLD_SNAKE_FORTUNE, cmd = GoldSnakeFortuneConstant.MsgBean.RES_START_GAME, resp = true)
@ProtoDesc("开始游戏返回")
public class ResGoldSnakeFortuneStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("累计中奖金币")
    public long allWinGold;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示  1.sweet   2.big   3.mega  4.epic  5.legendary")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("中奖图标信息")
    public List<GoldSnakeFortuneWinIconInfo> winIconInfoList;
    @ProtoDesc("从奖池获得的奖励")
    public long rewardPoolValue;
    @ProtoDesc("状态  0.普通  1.免费模式  2.真免费   3.假免费")
    public int status;
    @ProtoDesc("免费模式累计奖励,免费模式最后一局才赋值")
    public long freeModeTotalReward;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("奖金图标信息")
    public List<GoldSnakeFortuneCoinInfo> coinInfoList;

    public ResGoldSnakeFortuneStartGame(int code) {
        super(code);
    }
}
