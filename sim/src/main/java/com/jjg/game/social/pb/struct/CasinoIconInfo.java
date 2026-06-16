package com.jjg.game.social.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 场景图标 (玩家信息卡用)。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage
@ProtoDesc("场景图标")
public class CasinoIconInfo {
    @ProtoDesc("场景id")
    public int casinoId;
    @ProtoDesc("是否已解锁")
    public boolean unlocked;
}
