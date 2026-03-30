package com.jjg.game.slots.game.wolfmoon.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WOLF_MOON, cmd = WolfMoonConstant.MsgBean.RES_ENTER_GAME, resp = true)
@ProtoDesc("请求配置信息响应")
public class ResWolfMoonConfigInfo extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("当前状态 0=正常 1=等待选择免费游戏 2=免费游戏中")
    public int status;
    @ProtoDesc("免费游戏类型")
    public int freeGameType;
    @ProtoDesc("剩余免费游戏次数")
    public int remainingFreeGames;
    @ProtoDesc("当前递增奖励倍数")
    public int currentMultiplier;
    @ProtoDesc("是否触发了免费游戏（等待选择中）")
    public boolean freeGameTriggered;

    public ResWolfMoonConfigInfo(int code) {
        super(code);
    }
}
