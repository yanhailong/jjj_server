package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

import java.util.List;

/**
 * 百家乐结算信息
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    resp = true,
    cmd = BaccaratMessageConstant.RespMsgBean.NOTIFY_BACCARAT_TABLE_SETTLEMENT_INFO
)
@ProtoDesc("百家乐结算信息")
public class NotifyBaccaratSettlementInfo extends AbstractNotice {

    @ProtoDesc("桌面的数据")
    public BaccaratTableInfo baccaratTableInfo;

    @ProtoDesc("场上结算信息，如果场上阶段不为结算，此值为空")
    public BaccaratSettlementInfo baccaratSettlementInfo;

    @ProtoDesc("结算时玩家赢的金币值")
    public List<BaccaratPlayerChangedGold> playerChangedGolds;
}
