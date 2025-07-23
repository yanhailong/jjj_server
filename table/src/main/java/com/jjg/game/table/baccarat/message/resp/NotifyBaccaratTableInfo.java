package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;

import java.util.List;

/**
 * 断线重连，通知玩家百家乐场上信息更新
 *
 * @author 2CL
 */
@ProtoDesc("断线重连 通知玩家百家乐牌桌信息")
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    resp = true,
    cmd = BaccaratMessageConstant.RespMsgBean.NOTIFY_BACCARAT_TABLE_INFO
)
public class NotifyBaccaratTableInfo extends AbstractNotice {

    @ProtoDesc("场上阶段信息")
    public EGamePhase gamePhase;

    @ProtoDesc("玩家第一次进入时初始化路单信息")
    public List<BaccaratCardState> cardStateList;

    @ProtoDesc("桌面的数据")
    public BaccaratTableInfo baccaratTableInfo;

    @ProtoDesc("结算时玩家赢的金币值")
    public List<PlayerChangedGold> playerChangedGolds;

    @ProtoDesc("场上结算信息，如果场上阶段不为结算，此值为空")
    public BaccaratSettlementInfo baccaratSettlementInfo;
}
