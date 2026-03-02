package com.jjg.game.slots.game.wolfmoon.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WOLF_MOON, cmd = WolfMoonConstant.MsgBean.RES_FREE_CHOOSE_ONE, resp = true)
@ProtoDesc("免费游戏选择响应")
public class ResWolfMoonFreeChooseOne extends AbstractResponse {

    /**
     * 免费游戏类型
     * 1-高赔付符号(12次) 2-固定堆叠百搭符号(8次) 3-递增奖励倍数(5次)
     */
    @ProtoDesc("免费游戏类型")
    public int freeGameType;

    /**
     * 剩余免费游戏次数
     */
    @ProtoDesc("剩余免费游戏次数")
    public int remainingFreeGames;

    /**
     * 当前递增奖励倍数（仅在递增奖励倍数模式下使用）
     */
    @ProtoDesc("当前递增奖励倍数")
    public int currentMultiplier;

    public ResWolfMoonFreeChooseOne(int code) {
        super(code);
    }
}
