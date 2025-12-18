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
 * @date 2025/12/2 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CHRISTMAS_NIGHT_TYPE, cmd = BasketballSuperstarConstant.MsgBean.RES_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResBasketballSuperstarStartGame extends AbstractResponse {
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
    public List<BasketballSuperstarIconInfo> rewardIconInfo;
    @ProtoDesc("免费转，随机图标 变成 wild 图标id  没开启默认为0")
    public int stickyIcon;
    @ProtoDesc("免费转，需要变成wild的 格子id 并保留到下一次免费转")
    public Set<Integer> changeStickyIconSet;

    public ResBasketballSuperstarStartGame(int code) {
        super(code);
    }
}
