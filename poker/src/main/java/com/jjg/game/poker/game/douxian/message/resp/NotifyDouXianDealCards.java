package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.bean.DouXianZonePlacementInfo;

import java.util.List;

/**
 * 开局/补牌通知，DESIGN.md 8.6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_DEAL_CARDS, resp = true)
@ProtoDesc("斗仙牌通知发牌")
public class NotifyDouXianDealCards extends AbstractNotice {
    @ProtoDesc("当前回合")
    public int round;
    @ProtoDesc("本次新发到手的牌(客户端牌id)，仅本人可见")
    public List<Integer> newCardIds;
    @ProtoDesc("补牌后手牌总数")
    public int handCardNum;
    @ProtoDesc("本回合已开放的区域(1凡界 2灵界 3仙界)")
    public List<Integer> openZoneIds;
    @ProtoDesc("出牌阶段结束时间")
    public long overTime;
    @ProtoDesc("自己的完整手牌(客户端牌id)，仅本人可见")
    public List<Integer> selfHandCardIds;
    @ProtoDesc("自己的三个区域完整状态，仅本人可见")
    public List<DouXianZonePlacementInfo> selfZonePlacements;
    @ProtoDesc("是否携带本人完整手牌/区域快照，用于兼容旧协议")
    public boolean hasSelfSnapshot;
}
