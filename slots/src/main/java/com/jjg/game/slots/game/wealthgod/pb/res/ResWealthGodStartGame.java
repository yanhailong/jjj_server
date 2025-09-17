package com.jjg.game.slots.game.wealthgod.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthgod.WealthGodConstant;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodSpinInfo;

import java.util.List;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_GOD, cmd = WealthGodConstant.MsgBean.RES_START_GAME, resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResWealthGodStartGame extends AbstractResponse {

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
    @ProtoDesc("获得的奖池金额")
    public long jackpotValue;
    @ProtoDesc("所有spin数据")
    public List<WealthGodSpinInfo> spinInfo;
    @ProtoDesc("中奖后的奖池金额")
    public long poolValue;

    public ResWealthGodStartGame(int code) {
        super(code);
    }
}
