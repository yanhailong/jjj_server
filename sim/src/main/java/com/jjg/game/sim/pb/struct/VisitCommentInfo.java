package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("拜访留言")
public class VisitCommentInfo {
    public String id;
    public long visitorId;
    public String visitorName;
    public int headImgId;
    public int headFrameId;
    public int casinoId;
    public String content;
    public int popularity;
    public long createTime;
}
