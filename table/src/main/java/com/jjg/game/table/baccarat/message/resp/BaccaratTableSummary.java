package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 百家乐房间摘要，进入场次时展示
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("百家乐房间摘要")
public class BaccaratTableSummary {

    @ProtoDesc("roomId")
    public long roomId;

    @ProtoDesc("百家乐游戏基础信息")
    public BaccaratBaseInfo baccaratBaseInfo;

    @ProtoDesc("牌型输赢状态")
    public List<BaccaratCardState> cardStateList;
}
