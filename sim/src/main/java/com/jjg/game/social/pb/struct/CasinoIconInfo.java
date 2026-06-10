package com.jjg.game.social.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 赌场图标 (玩家信息卡用)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage
@ProtoDesc("赌场图标")
public class CasinoIconInfo {
    @ProtoDesc("赌场id")
    public int casinoId;
    @ProtoDesc("是否已解锁")
    public boolean unlocked;
}
