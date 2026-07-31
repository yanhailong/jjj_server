package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.bean.DouXianPlayerInfo;
import com.jjg.game.room.constant.EGamePhase;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.REPS_DOU_XIAN_ROOM_BASE_INFO, resp = true)
@ProtoDesc("斗仙牌响应房间基本信息")
public class RepsDouXianRoomBaseInfo extends AbstractNotice {
    @ProtoDesc("当前阶段")
    public EGamePhase phase;
    @ProtoDesc("当前回合(1~4)")
    public int round;
    @ProtoDesc("当前回合倍率")
    public int roundMultiplier;
    @ProtoDesc("玩家信息")
    public List<DouXianPlayerInfo> playerInfos;
    @ProtoDesc("自己的手牌(客户端牌id)，仅本人可见")
    public List<Integer> selfHandCardIds;
    @ProtoDesc("当前阶段结束时间")
    public long overTime;
    @ProtoDesc("房间最低输赢")
    public long minWinLimit;
    @ProtoDesc("房间输赢封顶")
    public long maxWinLimit;
    @ProtoDesc("底分")
    public long betBase;
    @ProtoDesc("匹配状态(0未匹配 1匹配中 2匹配成功 3匹配超时)")
    public int matchState;
    @ProtoDesc("匹配倒计时结束时间戳(毫秒)，非匹配中为0")
    public long matchEndTime;
    @ProtoDesc("当前已入座人数")
    public int matchPlayerNum;
    @ProtoDesc("匹配目标人数")
    public int matchMaxPlayerNum;
}
