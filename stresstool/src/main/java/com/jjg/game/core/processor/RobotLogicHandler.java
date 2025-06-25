package com.jjg.game.core.processor;

import com.jjg.game.core.concurrent.BaseHandler;
import com.jjg.game.core.net.message.SMessage;
import com.jjg.game.core.robot.RobotThread;

/**
 * 机器人逻辑Handler，分线程绑定时使用此handler
 *
 * @author 2CL
 */
public class RobotLogicHandler extends BaseHandler {
    private final RobotThread robot;
    private final SMessage sMsg;

    public RobotLogicHandler(RobotThread robot, SMessage sMsg) {
        this.robot = robot;
        this.sMsg = sMsg;
    }

    @Override
    public void action() throws Exception {
        robot.getWindow().getCtx().addReceiveMsgs();
        robot.addRespMsg(sMsg);
    }
}