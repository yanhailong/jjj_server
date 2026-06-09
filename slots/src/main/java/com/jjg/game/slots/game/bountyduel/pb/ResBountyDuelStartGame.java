package com.jjg.game.slots.game.bountyduel.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.bountyduel.BountyDuelConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BOUNTY_DUEL_TYPE, cmd = BountyDuelConstant.MsgBean.RES_START_GAME, resp = true)
@ProtoDesc("返回赏金大对决开始游戏结果")
public class ResBountyDuelStartGame extends AbstractResponse {
    @ProtoDesc("图标列表")
    public List<Integer> iconList;
    @ProtoDesc("本局赢分")
    public long allWinGold;
    @ProtoDesc("免费模式累计赢分")
    public long totalWinGold;
    @ProtoDesc("当前状态")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示ID")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("玩家经验")
    public long exp;
    @ProtoDesc("首屏中奖图标信息")
    public BountyDuelIconInfo rewardIconInfo;
    @ProtoDesc("消除下落信息")
    public List<BountyDuelCascade> addIconInfoList;

    public ResBountyDuelStartGame(int code) {
        super(code);
    }
}
