package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.bean.DouXianPlayerInfo;

import java.util.List;

/**
 * 飞升结果通知(凡->灵->仙，仙界弃回公共牌库)，DESIGN.md 8.10
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_TIER_ADVANCE, resp = true)
@ProtoDesc("斗仙牌通知飞升结果")
public class NotifyDouXianTierAdvance extends AbstractNotice {
    @ProtoDesc("飞升后各玩家的区域摆牌状态")
    public List<DouXianPlayerInfo> playerInfos;
    @ProtoDesc("本次仙界舍弃回公共牌库的牌数(所有玩家合计)")
    public int discardedToPoolCount;
    @ProtoDesc("是否为第一回合(用于仙界解锁特效)")
    public boolean firstRound;
}
