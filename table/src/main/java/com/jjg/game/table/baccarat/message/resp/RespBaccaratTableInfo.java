package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;

import java.util.List;

/**
 * 通知百家乐当前场上信息，玩家第一次进入初始化界面信息
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    resp = true,
    cmd = BaccaratMessageConstant.RespMsgBean.RESP_BACCARAT_TABLE_INFO
)
@ProtoDesc("返回百家乐桌上信息 首次进入")
public class RespBaccaratTableInfo extends AbstractResponse {

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

    @ProtoDesc("压分列表")
    public List<Integer> betInfoList;

    @ProtoDesc("玩家总人数")
    public int playerTotalNum;

    public RespBaccaratTableInfo(int code) {
        super(code);
    }
}
