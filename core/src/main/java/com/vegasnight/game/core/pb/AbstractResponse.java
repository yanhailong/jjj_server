package com.vegasnight.game.core.pb;

import com.vegasnight.game.common.proto.ProtoDesc;

/**
 * @author 11
 * @date 2025/5/24 14:34
 */
public class AbstractResponse extends AbstractMessage{
    @ProtoDesc("状态码")
    public int code;

    public AbstractResponse(int code) {
        this.code = code;
    }
}
