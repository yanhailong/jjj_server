package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 活动道具掉落信息
 *
 * @author 2CL
 */
@ProtobufMessage
public class ActivityItemDropInfo {

    @ProtoDesc("道具信息")
    public List<KVInfo> itemMap;

    @ProtoDesc("活动ID")
    public long activityId;

    @ProtoDesc("活动类型")
    public int activityType;

    @ProtoDesc("游戏类型 1. slots，2. 百人押注 3. poker对战")
    public int gameType;
}
