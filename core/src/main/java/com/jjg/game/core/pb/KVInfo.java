package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/9/9 15:45
 */
@ProtobufMessage
@ProtoDesc("键值对")
public class KVInfo {
    public int key;
    public int value;

    public KVInfo() {
    }

    public KVInfo(int key, int value) {
    }
}
