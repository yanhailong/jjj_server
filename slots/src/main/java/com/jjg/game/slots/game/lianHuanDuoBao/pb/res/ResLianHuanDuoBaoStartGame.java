package com.jjg.game.slots.game.lianHuanDuoBao.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.lianHuanDuoBao.constant.LianHuanDuoBaoConstant;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.bean.LianHuanDuoBaoCascade;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.bean.LianHuanDuoBaoChestRewardPb;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.bean.LianHuanDuoBaoWinIconInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LIAN_HUAN_DUO_BAO_TYPE, cmd = LianHuanDuoBaoConstant.MsgBean.RES_START_GAME, resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResLianHuanDuoBaoStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表（一维 row-major，从顶到底 / 左到右）")
    public List<Integer> iconList;
    @ProtoDesc("本局中奖金币")
    public long allWinGold;
    @ProtoDesc("当前状态 0.正常 2.bonus")
    public int status;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示 1.sweet 2.big 3.mega 4.epic 5.legendary")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("本局收集到的钥匙数")
    public int collectedKeyNum;
    @ProtoDesc("结算后玩家累计钥匙数（0..14；达到 15 会清零并切下一关）")
    public int totalKeyNum;
    @ProtoDesc("本局所在关卡 1-3")
    public int layerNumber;
    @ProtoDesc("下局所在关卡")
    public int nextLayerNumber;
    @ProtoDesc("结算后玩家累计龙珠数")
    public int dragonBallCount;
    @ProtoDesc("本局聚宝盆增减量（正=进入；负=释放）")
    public long treasureBowlDelta;
    @ProtoDesc("结算后聚宝盆余额")
    public long treasureBowlAmount;
    @ProtoDesc("中奖图标信息（首次中奖）")
    public LianHuanDuoBaoWinIconInfo rewardIconInfo;
    @ProtoDesc("消除后添加图标的信息（每次 cascade 一项）")
    public List<LianHuanDuoBaoCascade> addIconInfoList;
    @ProtoDesc("本局开启的宝箱奖励列表（按开启顺序）")
    public List<LianHuanDuoBaoChestRewardPb> chestRewards;

    public ResLianHuanDuoBaoStartGame(int code) {
        super(code);
    }
}
