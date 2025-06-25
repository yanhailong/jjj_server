package com.jjg.game.logic.login.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventType;
import com.jjg.game.core.event.FuncTestEvent;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.core.net.message.SMessage;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.logic.ReqOnceOrder;
import com.jjg.game.pbmsg.hall.ReqLogin;
import com.jjg.game.pbmsg.hall.ResLogin;
import com.jjg.game.utils.LoggerUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 请求登录
 *
 * @author 2CL
 */
@FuncTestEvent(eventT = EventType.REQUEST_ONCE, functionT = FunctionType.NULL, order = ReqOnceOrder.REQ_LOGIN)
public class ReqLoginEvent extends AbstractEvent<ReqLogin> {

    public ReqLoginEvent(RobotThread robot) {
        super(robot);
        this.resOrder = MessageConst.CertifyMessage.RES_LOGIN;
    }

    @Override
    public void action(ReqLogin reqLogin, Object... obj) throws Exception {
        String token = robot.token;
        if (StringUtils.isEmpty(token)) {
            LoggerUtils.LOGGER.error("token is empty token is empty token is empty!");
            return;
        }
        reqLogin.playerId = robot.getPlayer().getPlayerInfo().getPid();
        reqLogin.token = token;
        LoggerUtils.LOGGER.info("机器人： {} 请求登录", reqLogin.playerId);
        SMessage msg = new SMessage(MessageConst.CertifyMessage.REQ_LOGIN, ProtostuffUtil.serialize(reqLogin), resOrder);
        sendMsg(msg);
    }
}
