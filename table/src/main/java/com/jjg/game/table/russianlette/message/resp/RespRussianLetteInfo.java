package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

import java.util.List;

/**
 * 通知俄罗斯转盘当前场上信息，玩家第一次进入初始化界面信息
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    resp = true,
    cmd = RussianLetteMessageConstant.RespMsgBean.RESP_RUSSIAN_LETTE_INFO
)
@ProtoDesc("返回俄罗斯转盘桌上信息 首次进入")
public class RespRussianLetteInfo extends AbstractResponse {

    @ProtoDesc("场上阶段信息")
    public EGamePhase gamePhase;

    @ProtoDesc("转盘结果 只记录最新12把的数字（0-36）")
    public List<Integer> cardStateList;

    @ProtoDesc("桌面的数据")
    public RussianLetteInfo russianletteTableInfo;

    @ProtoDesc("结算时玩家赢的金币值")
    public List<PlayerChangedGold> playerChangedGolds;

    @ProtoDesc("场上结算信息，如果场上阶段不为结算，此值为空")
    public RussianLetteSettlementInfo russianletteSettlementInfo;

    @ProtoDesc("压分列表")
    public List<Integer> betInfoList;

    @ProtoDesc("玩家总人数")
    public int playerTotalNum;

    @ProtoDesc("概率信息")
    public RussianLetteProb prob;

    @ProtoDesc("玩家牌桌玩的次数")
    public double playNum;

    @ProtoDesc("累计这次下注总金额（断线重连同步）")
    public double allBet;


    public RespRussianLetteInfo(int code) {
        super(code);
    }
}
