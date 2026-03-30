package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/7/30 10:22
 */
@ProtobufMessage
@ProtoDesc("滚轴信息")
public class RollerInfo {
    @ProtoDesc("列")
    public int column;
    @ProtoDesc("初始化格子")
    public List<ListInfo> initGrid;
    @ProtoDesc("滚轴数量和范围")
    public List<RollerScopeInfo> axleCountScope;
    @ProtoDesc("元素列表")
    public List<Integer> elements;
}
