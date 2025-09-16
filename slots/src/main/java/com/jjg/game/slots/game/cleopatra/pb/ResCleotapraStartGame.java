package com.jjg.game.slots.game.cleopatra.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.cleopatra.CleopatraConstant;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CLEOPATRA, cmd = CleopatraConstant.MsgBean.RES_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResCleotapraStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("累计中奖金币")
    public long allWinGold;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示  1.sweet   2.big   3.mega  4.epic  5.legendary")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("中奖图标id")
    public List<Integer> winIcons;
    @ProtoDesc("中奖图标坐标")
    public List<Integer> winIconList;
    @ProtoDesc("新增列信息")
    public List<CleopatraAddColumInfo> addColumInfoList;
    @ProtoDesc("从奖池获得的奖励")
    public long rewardPoolValue;
    @ProtoDesc("奖池奖励后，当前奖池的值")
    public long poolValue;

    public ResCleotapraStartGame(int code) {
        super(code);
    }
}
