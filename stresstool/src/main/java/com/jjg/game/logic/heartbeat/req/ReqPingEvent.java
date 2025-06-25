package com.jjg.game.logic.heartbeat.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.ReqHeartBeat;
import com.jjg.game.common.pb.ResHeartBeat;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventType;
import com.jjg.game.core.event.FuncTestEvent;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.core.net.message.SMessage;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.logic.heartbeat.ReqHeartBeatOrder;

/**
 * 心跳请求
 *
 * @author 2CL
 */
@FuncTestEvent(eventT = EventType.REQUEST_REPEAT, functionT = FunctionType.HEART_BEAT, order = ReqHeartBeatOrder.PING)
public class ReqPingEvent extends AbstractEvent<ReqHeartBeat> {

    public ReqPingEvent(RobotThread robot) {
        super(robot);
        this.resOrder = MessageConst.ToClientConst.RES_HEART_BEAT;
    }

    @Override
    public void action(ReqHeartBeat msgEntity, Object... obj) throws Exception {
        SMessage msg = new SMessage(MessageConst.ToClientConst.REQ_HEART_BEAT, ProtostuffUtil.serialize(msgEntity), resOrder);
        sendMsg(msg);
    }
}
