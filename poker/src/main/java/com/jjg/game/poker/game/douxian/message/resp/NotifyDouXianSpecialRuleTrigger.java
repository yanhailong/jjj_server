package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.bean.DouXianSpecialRuleInfo;

import java.util.List;

/**
 * 得证大道/隐忍渡劫触发通知，DESIGN.md 8.13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_SPECIAL_RULE_TRIGGER, resp = true)
@ProtoDesc("斗仙牌通知特殊规则触发")
public class NotifyDouXianSpecialRuleTrigger extends AbstractNotice {
    @ProtoDesc("本回合触发的特殊规则列表")
    public List<DouXianSpecialRuleInfo> ruleInfos;
}
