package com.jjg.game.table.loongtigerwar.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;
import com.jjg.game.table.loongtigerwar.message.LoongTigerWarMessageConstant;

import java.util.List;

/**
 * 通知龙虎斗当前场上信息，玩家第一次进入初始化界面信息
 *
 * @author lm
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.LOONG_TIGER_WAR_TYPE,
        cmd = LoongTigerWarMessageConstant.RespMsgBean.NOTIFY_LOONG_TIGER_WAR_INFO,
        resp = true
)
@ProtoDesc("通知龙虎斗桌上信息")
public class NotifyLoongTigerWarInfo extends AbstractNotice {

    @ProtoDesc("场上阶段信息")
    public EGamePhase gamePhase;

    @ProtoDesc("场上倒计时结束时间戳")
    public long tableCountDownTime;

    @ProtoDesc("区域下注信息")
    public List<BetTableInfo> tableAreaInfos;

    @ProtoDesc("当前房间的历史记录")
    public List<Integer> histories;

    @ProtoDesc("结算信息")
    public NotifyLoongTigerWarSettleInfo settleInfos;

    @ProtoDesc("押分列表")
    public List<Integer> betPointList;

    @ProtoDesc("前6玩家信息")
    public List<TablePlayerInfo> playerInfos;
}
