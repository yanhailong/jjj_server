package com.jjg.game.activepass.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import java.util.List;

@ProtobufMessage
@ProtoDesc("活跃通行证状态；passId为0表示没有开启的通行证")
public class ActivePassInfo {
    public int passId;
    public long endTime;
    public int level;
    public long points;
    public int purchasedPoints;
    /** 位标记：2初级、4高级，分别解锁。 */
    public int purchasedTracks;
    public int day;
    public List<ActivePassTaskInfo> tasks;
    public List<ActivePassRewardInfo> rewards;
}
