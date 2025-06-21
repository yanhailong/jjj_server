package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/6/21 9:27
 */
@ProtobufMessage
@ProtoDesc("保险箱信息")
public class SafeBoxInfo {
    @ProtoDesc("坐标")
    public int indexId;
    @ProtoDesc("倍数")
    public int times;
    @ProtoDesc("获得金币数量")
    public long addGold;
}
