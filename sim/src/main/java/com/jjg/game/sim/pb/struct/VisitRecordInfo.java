package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("拜访操作记录")
public class VisitRecordInfo {
    public String id;
    public long visitorId;
    public String visitorName;
    public int headImgId;
    public int headFrameId;
    public int casinoId;
    public int type;
    public int gameType;
    public int giftId;
    public int popularity;
    public long commissionGold;
    public long createTime;
}
