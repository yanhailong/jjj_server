package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.douxian.message.bean.DouXianPairSettlementInfo;
import com.jjg.game.poker.game.douxian.message.bean.DouXianRevealInfo;

import java.util.List;

/**
 * 单回合结算通知(6组两两结算，凡->灵->仙顺序播放)，DESIGN.md 8.9/四
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_SETTLEMENT, resp = true)
@ProtoDesc("斗仙牌通知结算结果")
public class NotifyDouXianSettlement extends AbstractNotice {
    @ProtoDesc("当前回合")
    public int round;
    @ProtoDesc("本回合所有玩家的完整亮牌信息(含牌面/牌型/灵力值)，结算已发生，不存在偷看问题，前端可以直接用这个渲染亮牌动画，不用再从pairResults里反推牌面")
    public List<DouXianRevealInfo> playerReveals;
    @ProtoDesc("两两结算结果列表")
    public List<DouXianPairSettlementInfo> pairResults;
    @ProtoDesc("结算完成后的玩家余额快照，前端用于牌桌金币刷新和最终校准")
    public List<PokerPlayerInfo> playerInfos;
}
