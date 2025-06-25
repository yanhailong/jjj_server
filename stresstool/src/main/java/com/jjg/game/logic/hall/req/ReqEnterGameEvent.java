package com.jjg.game.logic.hall.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventType;
import com.jjg.game.core.event.FuncTestEvent;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.core.net.message.SMessage;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.logic.hall.HallReqOrder;
import com.jjg.game.pbmsg.hall.ReqChooseGame;
import com.jjg.game.pbmsg.hall.StressMsgConst;

/**
 * @author Administrator
 */
@FuncTestEvent(eventT = EventType.REQUEST_REPEAT, functionT = FunctionType.HALL, order = HallReqOrder.REQ_CHOOSE_GAME)
public class ReqEnterGameEvent extends AbstractEvent<ReqChooseGame> {

    public ReqEnterGameEvent(RobotThread robot) {
        super(robot, StressMsgConst.HallMsgBean.REQ_ENTER_GAME);
    }

    @Override
    public void action(ReqChooseGame msgEntity, Object... obj) throws Exception {
        msgEntity.gameType = 20050001;
        SMessage msg = new SMessage(StressMsgConst.HallMsgBean.REQ_ENTER_GAME, ProtostuffUtil.serialize(msgEntity), resOrder);
        sendMsg(msg);
    }
}
