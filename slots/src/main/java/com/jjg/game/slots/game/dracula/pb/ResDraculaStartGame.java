package com.jjg.game.slots.game.dracula.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.slots.game.dracula.DraculaConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DRACULA_TYPE, cmd = DraculaConstant.MsgBean.RES_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResDraculaStartGame extends AbstractResponse {
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
    @ProtoDesc("大奖展示  1.sweet   2.big   3.mega  4.epic  5.legendary")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("中奖图标信息")
    public DraculaIconInfo rewardIconInfo;
    @ProtoDesc("消除后添加图标的信息")
    public List<DraculaCascade> addIconInfoList;
    @ProtoDesc("本局应用的乘倍值（免费模式从2开始，每次能量满+2）")
    public int multiplier;
    @ProtoDesc("本局结束后的能量值（0..maxEnergyAfter）")
    public int energyAfter;
    @ProtoDesc("本局结束后的能量满值上限（6/8/10/12/14/16）")
    public int maxEnergyAfter;
    @ProtoDesc("本局通过+1符号增加的免费次数（用于客户端 +N 动画）")
    public int addFreeCount;


    public ResDraculaStartGame(int code) {
        super(code);
    }
}
