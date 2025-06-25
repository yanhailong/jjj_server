package com.jjg.game.logic.hall.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventType;
import com.jjg.game.core.event.FuncTestEvent;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.pbmsg.hall.ResChooseGame;
import com.jjg.game.pbmsg.hall.StressMsgConst;
import com.jjg.game.utils.GsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Administrator
 */
@FuncTestEvent(eventT = EventType.RESPONSE, functionT = FunctionType.NULL, order = StressMsgConst.HallMsgBean.RES_ENTER_GAME)
public class ResEnterGameEvent extends AbstractEvent<ResChooseGame> {
    private static final Logger log = LoggerFactory.getLogger(ResEnterGameEvent.class);

    public ResEnterGameEvent(RobotThread robot) {
        super(robot);
    }

    @Override
    public void action(ResChooseGame msgEntity, Object... obj) throws Exception {
        log.info(GsonUtils.toJson(msgEntity));
    }
}
