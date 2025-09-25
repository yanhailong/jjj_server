package com.jjg.game.common.pb;

import com.jjg.game.common.proto.ProtoDesc;

/**
 * 节点间同步通信
 * @author lm
 * @date 2025/9/25 14:13
 */
public class AbsNodeMessage extends AbstractMessage{
    @ProtoDesc("请求id")
    public long reqId;
}
