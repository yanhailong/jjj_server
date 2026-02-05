package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 俄罗斯转盘牌型状态
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("俄罗斯转盘牌型状态")
public class RussianLetteCardState {

    @ProtoDesc("转盘数据")
    public int diceData;
}
