package com.jjg.game.slots.game.superstar.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superstar.SuperStarConstant;
import com.jjg.game.slots.game.superstar.pb.SuperStarSpinInfo;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_STAR_TYPE, cmd = SuperStarConstant.MsgBean.RES_START_GAME, resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResSuperStarStartGame extends AbstractResponse {

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
    @ProtoDesc("spin数据")
    public SuperStarSpinInfo spinInfo;

    public ResSuperStarStartGame(int code) {
        super(code);
    }
}
