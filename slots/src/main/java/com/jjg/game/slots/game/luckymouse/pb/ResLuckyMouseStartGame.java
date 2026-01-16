package com.jjg.game.slots.game.luckymouse.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.luckymouse.LuckyMouseConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LUCKY_MOUSE, cmd = LuckyMouseConstant.MsgBean.RES_LUCKY_MOUSE_START_GAME, resp = true)
@ProtoDesc("返回开始游戏信息")
public class ResLuckyMouseStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("累计中奖金币")
    public long allWinGold;
    @ProtoDesc("状态  0.普通   1.真福鼠  2.假福鼠")
    public int status;
    @ProtoDesc("剩余福鼠(免费)次数")
    public int remainFreeCount;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示")
    public int bigWinShow;
    @ProtoDesc("从奖池获得的奖励")
    public long rewardPoolValue;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("中奖图标信息")
    public List<LuckyMouseWinIconInfo> winIconInfoList;

    public ResLuckyMouseStartGame(int code) {
        super(code);
    }
}
