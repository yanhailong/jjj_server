package com.vegasnight.game.hall.listener;

import com.vegasnight.game.common.cluster.ClusterClient;
import com.vegasnight.game.common.cluster.ClusterMessage;
import com.vegasnight.game.common.cluster.ClusterSystem;
import com.vegasnight.game.common.listener.SessionCloseListener;
import com.vegasnight.game.common.listener.SessionEnterListener;
import com.vegasnight.game.common.listener.SessionLoginListener;
import com.vegasnight.game.common.listener.SessionLogoutListener;
import com.vegasnight.game.common.message.SessionKickout;
import com.vegasnight.game.common.protostuff.MessageUtil;
import com.vegasnight.game.common.protostuff.PFSession;
import com.vegasnight.game.common.protostuff.ProtostuffUtil;
import com.vegasnight.game.core.constant.Code;
import com.vegasnight.game.core.dao.TokenDao;
import com.vegasnight.game.core.data.CommonResult;
import com.vegasnight.game.core.data.Player;
import com.vegasnight.game.core.data.PlayerSessionInfo;
import com.vegasnight.game.core.pb.ReqLogin;
import com.vegasnight.game.core.pb.ResLogin;
import com.vegasnight.game.core.service.PlayerSessionService;
import com.vegasnight.game.hall.service.PlayerService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author 11
 * @date 2025/5/26 16:42
 */
@Component
public class PlayerEventListener implements SessionCloseListener, SessionEnterListener, SessionLoginListener, SessionLogoutListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private TokenDao tokenDao;
    @Autowired
    private ClusterSystem clusterSystem;

    @Override
    public void login(PFSession session, byte[] data) {
        ReqLogin req = ProtostuffUtil.deserialize(data, ReqLogin.class);
        ResLogin res = new ResLogin(Code.SUCCESS);
        try{
            log.debug("大厅节点收到校验token的请求 token = {}",req.token);
            if(StringUtils.isEmpty(req.token)){
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("参数不能为空,校验token失败");
                session.verifyPassFail();
                return;
            }

            Long playerId = tokenDao.getPlayerIdByToken(req.token);
            if(playerId == null || playerId < 1){
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("token无效，校验token失败");
                session.verifyPassFail();
                return;
            }

            CommonResult<Player> playerResult = playerService.loginAndNewOrSave(playerId , p -> {
                p.setId(playerId);
            });

            if(playerResult.code != Code.SUCCESS){
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("获取玩家失败，校验token失败");
                session.verifyPassFail();
                return;
            }

            Player player = playerResult.data;

            session.verifyPass(player.getId(), player.getIp(), null);

            changeSessionInfo(session, player);

            res.playerId = playerId;
            session.send(res);
            log.info("玩家登录成功 playerId = {}",player.getId());

            tokenDao.removeToken(req.token);
        }catch (Exception e){
            log.error("",e);
        }
    }

    @Override
    public void logout(long playerId, String sessionId) {
        playerSessionService.remove(playerId);
        log.info("玩家登出 playerId={}",playerId);
    }

    @Override
    public void sessionClose(PFSession session) {

    }

    @Override
    public void sessionEnter(PFSession session, long playerId) {

    }

    private void changeSessionInfo(PFSession pfSession,Player player){
        PlayerSessionInfo info = playerSessionService.getInfo(player.getId());
        if(info != null){
            if(Objects.equals(pfSession.sessionId(),info.getSessionId())){
                log.debug("session相同，不处理  session={},playerId={}",pfSession.sessionId(),player.getId());
                return;
            }
            //顶号
            ClusterClient clusterClient = clusterSystem.getClusterByPath(info.getNodeName());
            if (clusterClient != null) {
                SessionKickout sessionKickout = new SessionKickout();
                sessionKickout.sessionId = info.getSessionId();
                sessionKickout.playerId = player.getId();
                ClusterMessage clusterMessage = new ClusterMessage(MessageUtil.getPFMessage(sessionKickout));
                try {
                    clusterClient.getConnect().write(clusterMessage);
                } catch (InterruptedException e) {
                    log.error("顶号切换节点失败", e);
                }
                //保存
                info.setPlayerId(player.getId());
                info.setSessionId(pfSession.sessionId());
                info.setNodeName(pfSession.gatePath);
                log.info("顶号成功! playerId = {}", player.getId());
            }
        }else {
            info = new PlayerSessionInfo();
            info.setNodeName(pfSession.gatePath);
            info.setPlayerId(player.getId());
            info.setSessionId(pfSession.sessionId());
        }
        playerSessionService.save(info);
    }
}
