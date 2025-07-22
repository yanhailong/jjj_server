package com.jjg.game.table.loongtigerwar.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/7/18 10:25
 */
@ProtobufMessage
@ProtoDesc("红黑大战玩家结算信息")
public class LoongTigerWarPlayerSettleInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("位置(从0开始从左往下;-1为在线玩家)")
    public int index;
    @ProtoDesc("获得的货币")
    public long amount;
}
