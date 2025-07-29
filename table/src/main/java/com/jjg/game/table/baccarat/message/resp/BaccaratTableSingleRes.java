package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.constant.EGamePhase;

/**
 * 百家乐房间摘要，进入场次时展示
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("百家乐房间摘要")
public class BaccaratTableSingleRes {

    @ProtoDesc("百家乐游戏基础信息")
    public BaccaratBaseInfo baccaratBaseInfo;

    @ProtoDesc("百家乐牌状态")
    public BaccaratCardState baccaratCardState;

    @ProtoDesc("对局ID")
    public int roundId;

    @ProtoDesc("是否需要清除路单, 在结算阶段告知下一回合服务器是否会重新洗牌")
    public boolean needClearRoad;
}
