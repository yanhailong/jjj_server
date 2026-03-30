package com.jjg.game.slots.game.angrybirds.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.angrybirds.constant.AngryBirdsConstant;
import com.jjg.game.slots.game.angrybirds.pb.bean.AngryBirdsReplaceInfo;
import com.jjg.game.slots.game.angrybirds.pb.bean.AngryBirdsWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ANGRY_BIRDS, cmd = AngryBirdsConstant.MsgBean.RES_ANGRY_BIRDS_START_GAME, resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResAngryBirdsStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("本局中奖金币")
    public long allWinGold;
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
    public List<AngryBirdsWinIconInfo> rewardIconInfo;
    @ProtoDesc("免费倍率")
    public int freeMultiplier;
    @ProtoDesc("替换图标信息")
    public List<AngryBirdsReplaceInfo> replaceInfo;

    public ResAngryBirdsStartGame(int code) {
        super(code);
    }
}
