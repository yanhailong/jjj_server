package com.jjg.game.hall.listener;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.listener.SessionLoginListener;
import com.jjg.game.common.listener.SessionLogoutListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerSessionToken;
import com.jjg.game.hall.pb.ReqLogin;
import com.jjg.game.hall.pb.ResLogin;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.hall.logger.HallLogger;
import com.jjg.game.hall.service.PlayerService;
import com.jjg.game.sample.GameListConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private HallLogger hallLogger;

    @Override
    public void login(PFSession session, byte[] data) {
        ReqLogin req = ProtostuffUtil.deserialize(data, ReqLogin.class);
        ResLogin res = new ResLogin(Code.SUCCESS);
        res.playerId = req.playerId;
        try{
            log.debug("大厅节点收到校验token的请求 playerId = {},token = {}",req.playerId,req.token);
            if(req.playerId < GameConstant.Common.playerBeginId){
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("玩家id不能小于{},登录失败,reqPlayerId={}",GameConstant.Common.playerBeginId,req.playerId);
                session.verifyPassFail();
                return;
            }

            if(StringUtils.isEmpty(req.token)){
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("参数不能为空,登录失败, playerId = {}",req.playerId);
                session.verifyPassFail();
                return;
            }

            //从数据库查询PlayerSessionToken对象信息
            PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(req.playerId);
            if(playerSessionToken == null){
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("没有从db中找到playerSessionToken对象,登录失败, playerId = {}",req.playerId);
                session.verifyPassFail();
                return;
            }

            //校验token
            if(!playerSessionToken.getToken().equals(req.token)){
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("token校验失败,登录失败, playerId = {},dbToken = {},reqToken = {}",req.playerId,playerSessionToken.getToken(),req.token);
                session.verifyPassFail();
                return;
            }

            //是否过期
            long now = System.currentTimeMillis();
            if(playerSessionToken.getExpireTime() < now){
                res.code = Code.EXPIRE;
                session.send(res);
                log.debug("token过期,登录失败, playerId = {},token = {}",req.playerId,req.token);
                session.verifyPassFail();
                return;
            }

            CommonResult<Player> playerResult = playerService.loginAndNewOrSave(req.playerId, new CorePlayerService.PlayerSaveCallback() {
                @Override
                public void newexe(Player player) throws UnsupportedEncodingException {
                    player.setNickName("player" + req.playerId);
                    player.setCreateTime(TimeHelper.nowInt());
                    player.setIp(session.getAddress().getHost());
                }

                @Override
                public void exe(Player player) throws UnsupportedEncodingException {
                    player.setIp(session.getAddress().getHost());
                }
            });

            if(playerResult.code != Code.SUCCESS){
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("redis操作player失败，校验token失败 playerId = {}",req.playerId);
                session.verifyPassFail();
                return;
            }

            Player player = playerResult.data;

            session.verifyPass(player.getId(), player.getIp(), null);

            playerSessionService.changeSessionInfo(session, player);

            res.playerId = player.getId();
            res.nickName = player.getNickName();
            res.gold = player.getGold();
            res.diamond = player.getDiamond();
            res.vipLevel = player.getVipLevel();

            //添加游戏列表
            res.gameList = addGameList();

            session.send(res);

            hallLogger.login(player, req.token, playerSessionToken.getLoginType());
            log.info("玩家登录成功 playerId = {}",player.getId());
        }catch (Exception e){
            res.code = Code.EXCEPTION;
            session.send(res);
            log.error("",e);
        }
    }

    /**
     * 游戏列表配置
     */
    private List<GameListConfig> addGameList(){
        try{
            List<GameListConfig> list = new ArrayList<>();
            for(GameListConfig c : GameListConfig.factory.getAllSamples()){
                list.add(c);
            }
            return list;
        }catch (Exception e){
            log.error("",e);
        }
        return Collections.emptyList();
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
        log.debug("玩家进入大厅节点 playerId={}",playerId);
    }
}
