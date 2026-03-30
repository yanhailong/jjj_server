package com.jjg.game.slots.game.wolfmoon.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WOLF_MOON, cmd = WolfMoonConstant.MsgBean.REQ_FREE_CHOOSE_ONE)
@ProtoDesc("请求免费游戏选择")
public class ReqWolfMoonFreeChooseOne extends AbstractMessage {
    /**
     * 免费游戏类型
     * 1-高赔付符号(12次) 2-固定堆叠百搭符号(8次) 3-递增奖励倍数(5次)
     */
    @ProtoDesc("免费游戏类型 1-高赔付符号 2-固定堆叠百搭符号 3-递增奖励倍数")
    public int freeGameType;
}
