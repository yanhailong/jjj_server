package com.jjg.game.activity.grandroulette.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.grandroulette.message.bean.GrandRouletteSubordinate;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2026/4/23 14:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.NOTIFY_BIND_SUBORDINATES_CHANGE, resp = true)
@ProtoDesc("大转盘绑定玩家信息变化奖励")
public class NotifyBindSubordinatesChange extends AbstractNotice {
    @ProtoDesc("绑定信息")
    public List<GrandRouletteSubordinate> bindSubordinates;
}
