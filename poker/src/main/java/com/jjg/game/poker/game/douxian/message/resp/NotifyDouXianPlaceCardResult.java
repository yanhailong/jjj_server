package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.bean.DouXianZonePlacementInfo;

import java.util.List;

/**
 * 摆牌结果通知，DESIGN.md 8.6/8.7
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_PLACE_CARD_RESULT, resp = true)
@ProtoDesc("斗仙牌通知摆牌结果")
public class NotifyDouXianPlaceCardResult extends AbstractNotice {
    @ProtoDesc("摆牌玩家id")
    public long playerId;
    @ProtoDesc("本次摆牌结果")
    public DouXianZonePlacementInfo placement;
    @ProtoDesc("摆牌后剩余手牌数(仅广播给其他玩家用，自己以selfHandCardIds为准)")
    public int remainHandCardNum;
    @ProtoDesc("摆牌后的完整本人手牌(客户端牌id)，仅本人可见")
    public List<Integer> selfHandCardIds;
    @ProtoDesc("摆牌后的本人三个区域完整状态，仅本人可见；请求失败时用于纠正客户端")
    public List<DouXianZonePlacementInfo> selfZonePlacements;
    @ProtoDesc("是否携带本人完整手牌/区域快照，用于兼容旧协议")
    public boolean hasSelfSnapshot;
}
