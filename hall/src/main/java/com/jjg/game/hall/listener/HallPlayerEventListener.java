package com.jjg.game.hall.listener;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.listener.SessionLoginListener;
import com.jjg.game.common.listener.SessionLogoutListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerLastGameInfoDao;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.hall.dao.HallRoomDao;
import com.jjg.game.hall.logger.HallLogger;
import com.jjg.game.hall.pb.req.ReqLogin;
import com.jjg.game.hall.pb.res.ResLogin;
import com.jjg.game.hall.pb.struct.GameListConfig;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.hall.service.HallService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GameListCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/5/26 16:42
 */
@Component
public class HallPlayerEventListener implements SessionCloseListener, SessionEnterListener, SessionLoginListener,
    SessionLogoutListener {
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
    private RedisLock redisLock;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private HallService hallService;
    @Autowired
    private PlayerLastGameInfoDao playerLastGameInfoDao;
    @Autowired
    private MailService mailService;

    @Override
    public void login(PFSession session, byte[] data) {
        ReqLogin req = ProtostuffUtil.deserialize(data, ReqLogin.class);
        ResLogin res = new ResLogin(Code.SUCCESS);
        res.playerId = req.playerId;
        try {
            log.debug("大厅节点收到校验token的请求 playerId = {},token = {}", req.playerId, req.token);
            if (req.playerId < GameConstant.Common.defaultPlayerBeginId) {
                res.code = Code.ERROR_REQ;
                session.send(res);
                log.debug("玩家id不能小于{},登录失败,reqPlayerId={}", GameConstant.Common.defaultPlayerBeginId, req.playerId);
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
                log.debug("token校验失败,登录失败, playerId = {},dbToken = {},reqToken = {}", req.playerId,
                    playerSessionToken.getToken(), req.token);
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

            //标记是否为注册的账号
            boolean[] register = new boolean[1];
            CommonResult<Player> playerResult = hallPlayerService.loginAndNewOrSave(req.playerId,
                player -> {
                    if (player.getCreateTime() == 0) {
                        player.setNickName("player" + req.playerId);
                        player.setCreateTime(TimeHelper.nowInt());
                        player.setIp(session.getAddress().getHost());
                        player.setLevel(1);

                        //设置默认装扮
                        player.setHeadImgId(hallService.getDefaultHeadImgId());
                        player.setHeadFrameId(hallService.getDefaultHeadFrameId());
                        player.setNationalId(hallService.getDefaultNationalId());
                        player.setTitleId(hallService.getDefaultTitlelId());
                        register[0] = true;
                    } else {
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

            session.verifyPass(player.getId(), player.getIp(), null);

            //检查重连
            if (reconnect(session, player)) {
                hallLogger.login(player, req.token, playerSessionToken.getLoginType());
                return;
            }

            //接收全服邮件
            mailService.playerGetServerMails(player.getId());

            playerSessionService.changeSessionInfo(session, player);

            res.playerId = player.getId();
            res.nickName = player.getNickName();
            res.gender = player.getGender();
            res.gold = player.getGold();
            res.diamond = player.getDiamond();
            res.vipLevel = player.getVipLevel();
            res.headImgId = player.getHeadImgId();
            res.headFrameId = player.getHeadFrameId();
            res.nationalId = player.getNationalId();
            res.titleId = player.getTitleId();
            //添加游戏列表
            res.gameList = addGameList();
            session.send(res);

            //发送登录日志
            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);
            //需要设置workId，否则hall模块所有的请求都将处于多线程环境，当连点发生时不能保证逻辑顺序执行
            session.setWorkId(player.getId());
            hallLogger.login(player, req.token, playerSessionToken.getLoginType());

            if (register[0]) {
                hallService.saveDefaultAvatar(req.playerId);
            }
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
            Map<Integer, GameStatus> gameStatusesMap = hallService.getGameStatusesMap();
            for (GameListCfg configCfg : GameDataManager.getGameListCfgList()) {
                if (Objects.nonNull(gameStatusesMap)) {
                    GameStatus gameStatus = gameStatusesMap.get(configCfg.getId());
                    if (Objects.nonNull(gameStatus)) {
                        GameListConfig gameListConfig = new GameListConfig();
                        gameListConfig.sid = gameStatus.gameId();
                        gameListConfig.name = configCfg.getName();
                        //TODO 配置和后台状态统一
                        gameListConfig.status = gameStatus.open() == 1 ? gameStatus.status() : 2;
                        gameListConfig.iconType = configCfg.getIconType();
                        list.add(gameListConfig);
                        continue;
                    }
                }
                GameListConfig gameListConfig = new GameListConfig();
                gameListConfig.sid = configCfg.getId();
                gameListConfig.name = configCfg.getName();
                gameListConfig.status = configCfg.getStatus();
                gameListConfig.iconType = configCfg.getIconType();
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

        playerSessionService.changeSessionInfo(session, player);
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
                player = hallPlayerService.checkAndSave(player.getId(), new DataSaveCallback<>() {
                    @Override
                    public void updateData(Player dataEntity) {
                    }

                    @Override
                    public Boolean updateDataWithRes(Player p) {
                        p.setRoomId(0);
                        p.setGameType(0);
                        p.setRoomCfgId(0);
                        return true;
                    }
                });
                log.debug("获取房间信息失败,重连进入房间失败 playerId={},gameType = {},roomId = {}", player.getId(),
                    player.getGameType(), player.getRoomId());
                result.code = Code.FAIL;
                result.data = player;
                return result;
            }

            log.debug("玩家重连开始进入房间 playerId = {},gameType = {},roomId = {}", player.getId(), player.getGameType(),
                player.getRoomId());
            String nodePath = room.getPath();
            if (StringUtils.isEmpty(nodePath)) {
                player = hallPlayerService.checkAndSave(player.getId(), new DataSaveCallback<>() {
                    @Override
                    public void updateData(Player dataEntity) {
                    }

                    @Override
                    public Boolean updateDataWithRes(Player p) {
                        p.setRoomId(0);
                        p.setGameType(0);
                        p.setRoomCfgId(0);
                        return true;
                    }
                });
                log.debug("房间节点为空，重连进入房间失败 playerId={},gameType = {},roomId = {}", player.getId(),
                    player.getGameType(), player.getRoomId());
                result.code = Code.FAIL;
                result.data = player;
                return result;
            }

            //获取房间所在节点
            MarsNode marsNode = clusterSystem.getNode(nodePath);
            if (marsNode == null) {
                log.warn("找不到源房间所在节点,开始寻找新服务的节点,playerId={},gameType = {},roomId={},nodePath={}", player.getId(),
                    player.getGameType(), player.getRoomId(), nodePath);
                String lockKey = roomDao.getLockName(player.getGameType(), player.getRoomId());
                int tryTime = GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES;
                redisLock.lock(lockKey, tryTime);
                try {
                    marsNode = nodeManager.getGameNodeByWeight(player.getGameType(), player.getId(),
                        player.getIp());
                    if (marsNode == null) {
                        log.warn("无可用节点，nodeType={},gameType={}", NodeType.GAME, player.getGameType());
                        result.code = Code.FAIL;
                        result.data = player;
                        return result;
                    }
                    room.setPath(marsNode.getNodePath());
                    roomDao.saveRoom(room);
                } catch (Exception e) {
                    log.warn("房间迁移重试 playerId={},gameType = {},roomId = {}",
                        player.getId(), player.getGameType(), player.getRoomId(), e);
                } finally {
                    redisLock.unlock(lockKey);
                }
            }

            if (marsNode == null) {
                log.debug("房间迁移失败，未找到新的节点 playerId={},gameType = {},roomId={}", player.getId(), player.getGameType(),
                    player.getRoomId());
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

    /**
     * 检查是否重连
     *
     * @param player
     * @return
     */
    private boolean reconnect(PFSession session, Player player) {
        boolean halfOffLine = false;
        Optional<PlayerLastGameInfo> op = playerLastGameInfoDao.findById(player.getId());
        PlayerLastGameInfo playerLastGameInfo = null;
        if (op.isPresent()) {
            playerLastGameInfo = op.get();
            if (playerLastGameInfo.isHalfwayOffline() && StringUtils.isNotEmpty(playerLastGameInfo.getNodePath())) {
                halfOffLine = true;
            }
        }

        if (!halfOffLine) {
            return false;
        }

        MarsNode node = clusterSystem.getNode(playerLastGameInfo.getNodePath());
        if (node == null) {
            node = nodeManager.getGameNodeByWeight(playerLastGameInfo.getGameType(), player.getId(), player.getIp());
            if (node == null) {
                log.info("重连时未发现该类型游戏节点 playerId={},gameType={}", player.getId(), player.getGameType());
                return false; //这里不能返回true，不然玩家无法登录
            }
        }
        log.info("玩家重连开始切换节点 playerId={},gameType={},toNode = {}", player.getId(), player.getGameType(),
            node.getNodePath());
        clusterSystem.switchNode(session, node);
        return true;
    }
}
