package com.jjg.game.slots.game.wealthbank.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/7/30 10:26
 */
@ProtobufMessage
@ProtoDesc("滚轴数量和范围")
public class WealthBankRollerScopeInfo {
    @ProtoDesc("滚轴id")
    public int id;
    @ProtoDesc("起始")
    public int begin;
    @ProtoDesc("结束")
    public int end;
}
