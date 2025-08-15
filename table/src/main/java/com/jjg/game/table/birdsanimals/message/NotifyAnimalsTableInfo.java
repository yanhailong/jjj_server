package com.jjg.game.table.birdsanimals.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.birdsanimals.constant.AnimalsConstant;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;

import java.util.List;

/**
 * 飞禽走兽桌面信息，下注，结算，断线重连
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BIRDS_ANIMAL_TYPE,
    cmd = AnimalsConstant.RespMsgBean.NOTIFY_ANIMALS_TABLE_INFO,
    resp = true
)
@ProtoDesc("飞禽走兽桌面信息，下注，结算，断线重连")
public class NotifyAnimalsTableInfo extends AbstractNotice {

    @ProtoDesc("场上阶段信息")
    public EGamePhase gamePhase;

    @ProtoDesc("场上倒计时结束时间戳")
    public long tableCountDownTime;

    @ProtoDesc("区域下注信息")
    public List<BetTableInfo> tableAreaInfos;

    @ProtoDesc("押分分值列表")
    public List<Integer> betPointList;

    @ProtoDesc("当前玩家信息")
    public TablePlayerInfo playerInfo;

    @ProtoDesc("房间总人数")
    public int totalPlayerNum;

    @ProtoDesc("结算历史，首次进入时发送，下注区域id列表")
    public List<AnimalsHistoryBean> settlementHistory;

    @ProtoDesc("结算信息，当阶段为结算时发送")
    public AnimalsSettlementInfo settlementInfo;

    @ProtoDesc("押注桌面展示的筹码数量上限，客户端在房间初始化时需要，需要在每个游戏初始化协议中下发此字段，需要注意")
    public int maxChipOnTable;
}
