package com.jjg.game.slots.game.lianHuanDuoBao.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.lianHuanDuoBao.constant.LianHuanDuoBaoConstant;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.bean.LianHuanDuoBaoPoolInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LIAN_HUAN_DUO_BAO_TYPE, cmd = LianHuanDuoBaoConstant.MsgBean.RES_ENTER_GAME, resp = true)
@ProtoDesc("返回配置信息")
public class ResLianHuanDuoBaoEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("当前状态 0.正常 2.bonus")
    public int status;
    @ProtoDesc("当前关卡 1-3")
    public int curLayer;
    @ProtoDesc("已收集的钥匙数（0..15）")
    public int collectedKeyNum;
    @ProtoDesc("已收集的龙珠数")
    public int dragonBallCount;
    @ProtoDesc("聚宝盆余额")
    public long treasureBowlAmount;
    @ProtoDesc("奖池信息")
    public List<LianHuanDuoBaoPoolInfo> poolList;

    public ResLianHuanDuoBaoEnterGame(int code) {
        super(code);
    }
}
