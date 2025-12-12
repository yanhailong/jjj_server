package com.jjg.game.slots.game.basketballSuperstar.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/12/2 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CHRISTMAS_NIGHT_TYPE, cmd = BasketballSuperstarConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("返回配置信息")
public class ResBasketballSuperstarEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("奖池信息")
    public List<BasketballSuperstarPoolInfo> poolList;

    public ResBasketballSuperstarEnterGame(int code) {
        super(code);
    }
}
