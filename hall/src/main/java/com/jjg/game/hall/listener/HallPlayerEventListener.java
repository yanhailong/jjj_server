package com.jjg.game.hall.listener;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.listener.SessionLoginListener;
import com.jjg.game.common.listener.SessionLogoutListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.*;
import com.jjg.game.hall.dao.HallRoomDao;
import com.jjg.game.hall.pb.GameListConfig;
import com.jjg.game.hall.pb.ReqLogin;
import com.jjg.game.hall.pb.ResLogin;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.hall.logger.HallLogger;
import com.jjg.game.hall.sample.GameDataManager;
import com.jjg.game.hall.sample.bean.GameListCfg;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.core.data.Room;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
public class HallPlayerEventListener implements SessionCloseListener, SessionEnterListener, SessionLoginListener, SessionLogoutListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private HallPlayerService hallPlayerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private HallLogger hallLogger;
    @Autowired
    private HallRoomDao roomDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisLock redisLock;
    @Autowired
    private NodeManager nodeManager;

    @Override
    public void login(PFSession session, byte[] data) {
        ReqLogin req = ProtostuffUtil.deserialize(data, ReqLogin.class);
        ResLogin res = new ResLogin(Code.SUCCESS);
        res.playerId = req.playerId;
        try {
            log.debug("大厅节点收到校验token的请求 playerId = {},token = {}", req.playerId, req.token);
            if (req.playerId < GameConstant.Common.playerBeginId) {
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("玩家id不能小于{},登录失败,reqPlayerId={}", GameConstant.Common.playerBeginId, req.playerId);
                session.verifyPassFail();
                return;
            }

            if (StringUtils.isEmpty(req.token)) {
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("参数不能为空,登录失败, playerId = {}", req.playerId);
                session.verifyPassFail();
                return;
            }

            //从数据库查询PlayerSessionToken对象信息
            PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(req.playerId);
            if (playerSessionToken == null) {
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("没有从db中找到playerSessionToken对象,登录失败, playerId = {}", req.playerId);
                session.verifyPassFail();
                return;
            }

            //校验token
            if (!playerSessionToken.getToken().equals(req.token)) {
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("token校验失败,登录失败, playerId = {},dbToken = {},reqToken = {}", req.playerId, playerSessionToken.getToken(), req.token);
                session.verifyPassFail();
                return;
            }

            //是否过期
            long now = System.currentTimeMillis();
            if (playerSessionToken.getExpireTime() < now) {
                res.code = Code.EXPIRE;
                session.send(res);
                log.debug("token过期,登录失败, playerId = {},token = {}", req.playerId, req.token);
                session.verifyPassFail();
                return;
            }

            CommonResult<Player> playerResult = hallPlayerService.loginAndNewOrSave(req.playerId, new CorePlayerService.PlayerSaveCallback() {
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

            if (playerResult.code != Code.SUCCESS) {
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("redis操作player失败，校验token失败 playerId = {}", req.playerId);
                session.verifyPassFail();
                return;
            }

            Player player = playerResult.data;

            if (player.getRoomId() > 0) {
                CommonResult<Player> result = enterRoom(session, player);
                if (result.code == Code.SUCCESS) {
                    return;
                }
                player = result.data;
            }

            session.verifyPass(player.getId(), player.getIp(), null);

            playerSessionService.changeSessionInfo(session, player);

            res.playerId = player.getId();
            res.nickName = player.getNickName();
            res.gold = player.getGold();
            res.diamond = player.getDiamond();
            res.vipLevel = player.getVipLevel();

            //添加游戏列表
            res.gameList = addGameList();
            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);
            session.send(res);

            hallLogger.login(player, req.token, playerSessionToken.getLoginType());
            log.info("玩家登录成功 playerId = {}", player.getId());
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            session.send(res);
            log.error("", e);
        }
    }

    /**
     * 游戏列表配置
     */
    private List<GameListConfig> addGameList() {
        try {
            List<GameListConfig> list = new ArrayList<>();
            for (GameListCfg configCfg : GameDataManager.getGameListCfgList()) {
                GameListConfig gameListConfig = new GameListConfig();
                gameListConfig.sid = configCfg.getSid();
                gameListConfig.name = configCfg.getName();
                gameListConfig.status = configCfg.getStatus();
                list.add(gameListConfig);
            }
            return list;
        } catch (Exception e) {
            log.error("", e);
        }
        return Collections.emptyList();
    }

    @Override
    public void logout(long playerId, String sessionId) {
        playerSessionService.remove(playerId);
        log.info("玩家登出 playerId={}", playerId);
    }

    @Override
    public void sessionClose(PFSession session) {
        session.setReference(null);
    }

    @Override
    public void sessionEnter(PFSession session, long playerId) {
        Player player = hallPlayerService.get(playerId);
        PlayerController playerController = new PlayerController(session, player);
        session.setReference(playerController);

        log.debug("玩家进入大厅节点 playerId={}", playerId);
    }

    /**
     * 重连进入房间
     *
     * @param session
     * @param player
     * @return
     */
    private CommonResult<Player> enterRoom(PFSession session, Player player) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        try {
            Room room = roomDao.getRoom(player.getGameType(), player.getRoomId());
            if (room == null) {
                player = hallPlayerService.checkAndSave(player.getId(), p -> {
                    p.setRoomId(0);
                    p.setGameType(0);
                    p.setWareId(0);
                    return true;
                });
                log.debug("获取房间信息失败,重连进入房间失败 playerId={},gameType = {},roomId = {}", player.getId(), player.getGameType(), player.getRoomId());
                result.code = Code.FAIL;
                result.data = player;
                return result;
            }

            log.debug("玩家重连开始进入房间 playerId = {},gameType = {},roomId = {}", player.getId(), player.getGameType(), player.getRoomId());
            String nodePath = room.getPath();
            if (StringUtils.isEmpty(nodePath)) {
                player = hallPlayerService.checkAndSave(player.getId(), p -> {
                    p.setRoomId(0);
                    p.setGameType(0);
                    p.setWareId(0);
                    return true;
                });
                log.debug("房间节点为空，重连进入房间失败 playerId={},gameType = {},roomId = {}", player.getId(), player.getGameType(), player.getRoomId());
                result.code = Code.FAIL;
                result.data = player;
                return result;
            }

            //获取房间所在节点
            MarsNode marsNode = clusterSystem.getNode(nodePath);
            if (marsNode == null) {
                log.warn("找不到源房间所在节点,开始寻找新服务的节点,playerId={},gameType = {},roomId={},nodePath={}", player.getId(), player.getGameType(), player.getRoomId(), nodePath);
                String lockKey = roomDao.getLockName(player.getGameType(), player.getRoomId());
                for(int i=0;i< CoreConst.Common.REDIS_TRY_COUNT;i++){
                    if(redisLock.lock(lockKey)){
                        try{
                            marsNode = nodeManager.loadGameNode(NodeType.GAME, player.getGameType(), player.getId(), player.getIp());
                            if (marsNode == null) {
                                log.warn("无可用节点，nodeType={},gameType={}", NodeType.GAME, player.getGameType());
                                result.code = Code.FAIL;
                                result.data = player;
                                return result;
                            }

                            room.setPath(marsNode.getNodePath());
                            roomDao.saveRoom(room);
                            break;
                        } catch (Exception e) {
                            log.warn("房间迁移重试 playerId={},gameType = {},roomId = {},retryCount = {}", player.getId(), player.getGameType(), player.getRoomId(), i, e);
                        } finally {
                            redisLock.unlock(lockKey);
                        }
                    }
                }
            }

            if (marsNode == null) {
                log.debug("房间迁移失败，未找到新的节点 playerId={},gameType = {},roomId={}", player.getId(), player.getGameType(), player.getRoomId());
                result.code = Code.FAIL;
                result.data = player;
                return result;
            }

            clusterSystem.switchNode(session, marsNode);
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        result.data = player;
        return result;
    }
}
