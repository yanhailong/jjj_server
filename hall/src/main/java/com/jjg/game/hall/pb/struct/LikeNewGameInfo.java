package com.jjg.game.hall.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/3/14
 */
@ProtobufMessage
@ProtoDesc("点赞信息")
public class LikeNewGameInfo {
    @ProtoDesc("游戏id")
    public int gameType;
    @ProtoDesc("点赞数量")
    public int nums;
    @ProtoDesc("是否已点赞")
    public boolean like;
}
