package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/21 9:27
 */
@ProtobufMessage
@ProtoDesc("美元信息")
public class DollarsInfo {
    @ProtoDesc("美元符号图标坐标，如果有正确坐标，则说明触发了美元现金奖励")
    public int coinIndexId;
    @ProtoDesc("钞票坐标")
    public List<Integer> dollarIndexIds;
    @ProtoDesc("钞票金额")
    public List<Long> dollarValueList;
}
