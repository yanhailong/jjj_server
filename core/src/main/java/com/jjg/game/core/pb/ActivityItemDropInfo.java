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
    public int activityId;
}
