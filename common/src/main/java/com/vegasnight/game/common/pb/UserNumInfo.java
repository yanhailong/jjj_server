package com.vegasnight.game.common.pb;

import com.vegasnight.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2023/3/9
 */
@ProtobufMessage(toPbFile = false)
public class UserNumInfo {
    public String nodeName;
    public int num;
}
