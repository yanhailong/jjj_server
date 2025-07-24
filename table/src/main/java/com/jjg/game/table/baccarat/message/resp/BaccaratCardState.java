package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 百家乐牌型状态
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("百家乐牌型状态")
public class BaccaratCardState {

    @ProtoDesc("输赢状态, 1：庄赢，2：闲赢，3：和")
    public byte winState;

    @ProtoDesc("牌型的输赢状态, 0：默认状态，1：庄对，2：闲对，3：庄和闲都有对子")
    public byte cardTypeWinState;

    @ProtoDesc("是否是天王牌")
    public boolean hasKingCard;
}
