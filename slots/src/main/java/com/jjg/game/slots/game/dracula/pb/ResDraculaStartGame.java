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
    @ProtoDesc("本局应用的最终乘倍值（主游戏 1/2/3/4/5；免费 3/6/9/12/15；按本局消除轮数查表，无中奖时为1）")
    public int multiplier;


    public ResDraculaStartGame(int code) {
        super(code);
    }
}
