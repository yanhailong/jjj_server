package com.jjg.game.logic.heartbeat.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.ResHeartBeat;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventType;
import com.jjg.game.core.event.FuncTestEvent;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.utils.LoggerUtils;

/**
 * 心跳返回
 *
 * @author 2CL
 */
@FuncTestEvent(eventT = EventType.RESPONSE, functionT = FunctionType.NULL, order = MessageConst.ToClientConst.RES_HEART_BEAT)
public class ResPingEvent extends AbstractEvent<ResHeartBeat> {

    public ResPingEvent(RobotThread robot) {
        super(robot);
        this.resOrder = MessageConst.ToClientConst.RES_HEART_BEAT;
    }

    @Override
    public void action(ResHeartBeat msgEntity, Object... obj) throws Exception {
        LoggerUtils.LOGGER.info("收到心跳当前时间：{}", msgEntity.time);
    }
}
