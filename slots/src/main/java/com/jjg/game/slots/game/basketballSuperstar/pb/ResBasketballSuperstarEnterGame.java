package com.jjg.game.slots.game.basketballSuperstar.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;

import java.util.List;
import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BASKETBALL_SUPERSTAR, cmd = BasketballSuperstarConstant.MsgBean.RES_CONFIG_INFO, resp = true)
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
    @ProtoDesc("已经转了免费次数")
    public int freeCount;

    @ProtoDesc("免费转，随机图标 变成 wild 图标id  没开启默认为0")
    public int stickyIcon;
    @ProtoDesc("免费转，新增变成wild")
    public Set<Integer> addStickyIconSet;
    @ProtoDesc("免费转，需要变成wild的 格子id 并保留到下一次免费转")
    public Set<Integer> changeStickyIconSet;
    @ProtoDesc("奖池信息")
    public List<BasketballSuperstarPoolInfo> poolList;

    public ResBasketballSuperstarEnterGame(int code) {
        super(code);
    }
}
