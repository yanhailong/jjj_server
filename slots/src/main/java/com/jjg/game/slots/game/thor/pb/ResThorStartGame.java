package com.jjg.game.slots.game.thor.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.cleopatra.pb.CleopatraWinIconInfo;
import com.jjg.game.slots.game.thor.ThorConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/12/1 18:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.THOR, cmd = ThorConstant.MsgBean.RES_START_GAME, resp = true)
@ProtoDesc("返回游戏信息")
public class ResThorStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
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
    @ProtoDesc("中奖图标信息")
    public List<ThorWinIconInfo> winIconInfoList;
    @ProtoDesc("从奖池获得的奖励")
    public long rewardPoolValue;
    @ProtoDesc("状态  0.普通   1.二选一   2.火焰   3.冰冻")
    public int status;
    @ProtoDesc("免费模式累计奖励,免费模式最后一局才赋值")
    public long freeModeTotalReward;
    @ProtoDesc("标记免费模式结束")
    public boolean freeEnd;

    public ResThorStartGame(int code) {
        super(code);
    }
}
