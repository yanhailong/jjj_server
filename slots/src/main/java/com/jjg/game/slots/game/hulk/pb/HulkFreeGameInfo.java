package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/4/24
 */
@ProtobufMessage
@ProtoDesc("免费游戏")
public class HulkFreeGameInfo {
    @ProtoDesc("状态  4.触发第3列wild  5.第3列wild  6.触发第234列wild  7.第234列wild")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
}
