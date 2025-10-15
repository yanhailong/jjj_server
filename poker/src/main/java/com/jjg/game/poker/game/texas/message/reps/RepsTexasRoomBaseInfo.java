package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.message.bean.TexasPlayerInfo;
import com.jjg.game.room.constant.EGamePhase;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/29 15:06
 */

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE
        , cmd = TexasConstant.MsgBean.REPS_ROOM_BASE_INFO, resp = true)
@ProtoDesc("响应房间基础信息")
public class RepsTexasRoomBaseInfo extends AbstractResponse {
    @ProtoDesc("阶段信息")
    public EGamePhase phase;
    @ProtoDesc("阶段结束时间")
    public long phaseEndTime;
    @ProtoDesc("玩家基础信息")
    public List<TexasPlayerInfo> playerInfos;
    @ProtoDesc("公牌")
    public List<Integer> publicCards;
    @ProtoDesc("等待操作用户id")
    public long waitPlayerId;
    @ProtoDesc("等待结束时间")
    public long waitEndTime;
    @ProtoDesc("底池")
    public long bottomPool;
    @ProtoDesc("结算信息")
    public NotifyTexasSettlementInfo notifyTexasSettlementInfo;
    @ProtoDesc("边池")
    public List<Long> edgePool;
    @ProtoDesc("庄家座位位置")
    public int seatId;
    @ProtoDesc("定庄动画时间")
    public int findDealerTime;
    @ProtoDesc("发牌一张时间")
    public int sendCardTime;
    @ProtoDesc("操作时间")
    public int operationTime;
    @ProtoDesc("大盲")
    public int BB;
    @ProtoDesc("小盲")
    public int SB;


    public RepsTexasRoomBaseInfo(int code) {
        super(code);
    }


}
