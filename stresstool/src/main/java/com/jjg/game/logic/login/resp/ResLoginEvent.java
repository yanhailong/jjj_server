package com.jjg.game.logic.login.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventType;
import com.jjg.game.core.event.FuncTestEvent;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.logic.robot.entity.PlayerInfo;
import com.jjg.game.logic.robot.entity.RobotPlayer;
import com.jjg.game.pbmsg.hall.ResLogin;

/**
 * 登录返回
 *
 * @author 2CL
 */
@FuncTestEvent(eventT = EventType.RESPONSE, functionT = FunctionType.NULL, order = MessageConst.CertifyMessage.RES_LOGIN)
public class ResLoginEvent extends AbstractEvent<ResLogin> {

    public ResLoginEvent(RobotThread robot) {
        super(robot);
        this.resOrder = MessageConst.CertifyMessage.RES_LOGIN;
    }

    @Override
    public void action(ResLogin resLogin, Object... obj) throws Exception {
        if (resLogin == null) {
            return;
        }
        robot.init = false;
        if (resLogin.code != Code.SUCCESS) {
            Log4jManager.getInstance().error("robotName:" + robot.getName() + "登陆失败 " + resLogin.code);
            robot.getChannel().close();
            return;
        }

        long loginTime = System.currentTimeMillis();
        RobotPlayer robotPlayer = robot.getPlayer();
        robotPlayer.setLoginTime(loginTime);
        robotPlayer.setLogin(true);

        PlayerInfo playerInfo = new PlayerInfo();
        robotPlayer.setPlayerInfo(playerInfo);
        playerInfo.setPlayerId(resLogin.playerId);
        playerInfo.setGold(resLogin.gold);
        playerInfo.setDiamond(resLogin.diamond);
        playerInfo.setNickName(resLogin.nickName);
        playerInfo.setVipLevel(resLogin.vipLevel);
        playerInfo.setGameList(resLogin.gameList);

        Log4jManager.getInstance().info(robot.getWindow(), "机器人:" + robot.getName() + "登录成功, name:" + playerInfo.getNickName() + ",id:" + playerInfo.getPid() + ",channelId:" + robot.getChannel().hashCode());
        robot.getWindow().getCtx().addLogined();
    }
}
