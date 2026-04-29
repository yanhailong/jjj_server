package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.hulk.HulkConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HULK, cmd = HulkConstant.MsgBean.RES_ENTER_GAME, resp = true)
@ProtoDesc("返回配置信息")
public class ResHulkEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("当前奖池的值")
    public long poolValue;
    @ProtoDesc("状态  0.普通  1.触发免费模式  2.免费模式  3.触发小游戏  4.触发第3列wild  5.第3列wild  6.触发第234列wild  7.第234列wild")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("免费模式累计奖励")
    public long freeModeTotalReward;
    @ProtoDesc("奖池配置信息")
    public List<HulkPoolInfo> poolList;
    @ProtoDesc("当status值为3时，该值才具有意义。 小游戏信息")
    public HulkMiniGame hulkMiniGame;
    @ProtoDesc("当status值为2时，该值才具有意义。 免费游戏信息")
    public HulkFreeGameInfo hulkFreeGameInfo;

    public ResHulkEnterGame(int code) {
        super(code);
    }
}
