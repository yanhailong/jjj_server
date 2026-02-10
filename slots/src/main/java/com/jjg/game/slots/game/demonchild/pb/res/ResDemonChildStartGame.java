package com.jjg.game.slots.game.demonchild.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.slots.game.demonchild.constant.DemonChildConstant;
import com.jjg.game.slots.game.demonchild.pb.bean.DemonChildLineInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CAPTAIN_JACK, cmd = DemonChildConstant.MsgBean.RES_DEMON_CHILD_START_GAME, resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResDemonChildStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("图标金额")
    public List<KVInfo> iconAmountList;
    @ProtoDesc("本局中奖金币")
    public long allWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费 ")
    public int status;
    @ProtoDesc("总免费次数")
    public int totalFreeCount;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示  1.sweet   2.big   3.mega  4.epic  5.legendary")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("中奖线信息")
    public List<DemonChildLineInfo> rewardLineInfo;

    public ResDemonChildStartGame(int code) {
        super(code);
    }
}
