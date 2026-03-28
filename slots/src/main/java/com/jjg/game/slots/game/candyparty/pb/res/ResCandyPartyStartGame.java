package com.jjg.game.slots.game.candyparty.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.candyparty.constant.CandyPartyConstant;
import com.jjg.game.slots.game.candyparty.pb.bean.CandyPartyCascade;
import com.jjg.game.slots.game.candyparty.pb.bean.CandyPartyWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CANDY_PARTY, cmd = CandyPartyConstant.MsgBean.RES_CANDY_PARTY_START_GAME, resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResCandyPartyStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("本局中奖金币")
    public long allWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费 2.探宝")
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
    @ProtoDesc("收集图标数量")
    public int collectedIconNum;
    @ProtoDesc("当前层数")
    public int layerNumber;
    @ProtoDesc("下轮层数")
    public int nextLayerNumber;
    @ProtoDesc("剩余图标数量")
    public int remainIconNum;
    @ProtoDesc("免费乘倍率")
    public int freeGameMultiple;
    @ProtoDesc("中奖图标信息")
    public CandyPartyWinIconInfo rewardIconInfo;
    @ProtoDesc("消除后添加图标的信息")
    public List<CandyPartyCascade> addIconInfoList;

    public ResCandyPartyStartGame(int code) {
        super(code);
    }
}
