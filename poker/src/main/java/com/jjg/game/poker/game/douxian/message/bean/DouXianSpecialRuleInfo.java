package com.jjg.game.poker.game.douxian.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 得证大道/隐忍渡劫触发信息，DESIGN.md 三 特殊插入点
 */
@ProtobufMessage
@ProtoDesc("斗仙牌特殊规则触发信息")
public class DouXianSpecialRuleInfo {
    @ProtoDesc("触发玩家id")
    public long playerId;
    @ProtoDesc("规则类型(1得证大道 2隐忍渡劫)")
    public int ruleType;
    @ProtoDesc("本回合触发全胜/全败的对手数量")
    public int triggerPlayerCount;
}
