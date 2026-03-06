package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;


/**
 * 通知俄罗斯转盘结算
 *
 * @author lhc
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    cmd = RussianLetteMessageConstant.RespMsgBean.RESP_RUSSIAN_LETTE_SETTLEMENT,
    resp = true
)
@ProtoDesc("通知俄罗斯转盘结算信息")
public class NotifyRussianLetteSettlement extends AbstractNotice {
    /** 当前游戏阶段：REST / BET / DRAW_ON / GAME_ROUND_OVER_SETTLEMENT */
    @ProtoDesc("当前阶段")
    public RussianLetteStageInfo stageInfo;

    @ProtoDesc("结算信息")
    public RussianLetteSettlementInfo settlementInfo;

    @ProtoDesc("概率信息")
    public RussianLetteProb prob;

    @ProtoDesc("玩家牌桌玩的次数")
    public int playNum;

    @ProtoDesc("累计这次下注总金额（断线重连同步）")
    public double allBet;
}
