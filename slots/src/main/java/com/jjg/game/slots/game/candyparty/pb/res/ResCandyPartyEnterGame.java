package com.jjg.game.slots.game.candyparty.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.candyparty.constant.CandyPartyConstant;
import com.jjg.game.slots.game.candyparty.pb.bean.CandyPartyPoolInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CANDY_PARTY, cmd = CandyPartyConstant.MsgBean.RES_CANDY_PARTY_ENTER_GAME, resp = true)
@ProtoDesc("返回配置信息")
public class ResCandyPartyEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费 2.探宝")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("当前累计免费金额")
    public long freeAmount;
    @ProtoDesc("免费乘倍率")
    public long freeMultiple;
    @ProtoDesc("当前第几层")
    public int curLayer;
    @ProtoDesc("当前收集的数量")
    public int curCollectNum;
    @ProtoDesc("奖池信息")
    public List<CandyPartyPoolInfo> poolList;

    public ResCandyPartyEnterGame(int code) {
        super(code);
    }
}
