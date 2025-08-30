package com.jjg.game.hall.listener;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.listener.SessionLoginListener;
import com.jjg.game.common.listener.SessionLogoutListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerLastGameInfoDao;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.pb.MarqueeInfo;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.hall.casino.service.PlayerBuildingService;
import com.jjg.game.hall.dao.HallRoomDao;
import com.jjg.game.hall.dao.LikeGameDao;
import com.jjg.game.hall.logger.HallLogger;
import com.jjg.game.hall.pb.req.ReqLogin;
import com.jjg.game.hall.pb.res.ResLogin;
import com.jjg.game.hall.pb.struct.GameWareInfo;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.hall.service.HallService;
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
    private NodeManager nodeManager;
    @Autowired
    private HallService hallService;
    @Autowired
    private PlayerLastGameInfoDao playerLastGameInfoDao;
    @Autowired
    private MailService mailService;
    @Autowired
    private CoreMarqueeManager marqueeManager;
    @Autowired
    private LikeGameDao likeGameDao;
    @Autowired
    private HallRoomDao hallRoomDao;
    @Autowired
    private PlayerBuildingService playerBuildingService;

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
                res.code = Code.EXPIRE;
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
                        player.setTitleId(hallService.getDefaultTitleId());
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
            res.chipsId = player.getChipsId();
            res.backgroundId = player.getBackgroundId();
            res.cardBackgroundId = player.getCardBackgroundId();
            //添加游戏列表
            res.gameList = hallService.getSortGameList();
            //添加跑马灯
            res.marqueeInfo = addMarquee();

            res.safeBoxGold = player.getSafeBoxGold();
            res.safeBoxDiamond = player.getSafeBoxDiamond();
            res.level = player.getLevel();
            res.exp = player.getExp();
            res.gameTypeList = likeGameDao.getLikeGames(player.getId());

            //更新session
            PlayerSessionInfo playerSessionInfo = playerSessionService.online(session, player);

            //检查重连
            if (reconnect(session, player, playerSessionInfo)) {
                res.gameWareInfo = new GameWareInfo();
                res.gameWareInfo.gameType = player.getGameType();
                res.gameWareInfo.roomCfgId = player.getRoomCfgId();
                session.send(res);
                hallLogger.login(player, req.token, playerSessionToken.getLoginType());
                return;
            }

            //返回登录消息
            session.send(res);
            hallLogger.login(player, req.token, playerSessionToken.getLoginType());

            //接收全服邮件
            mailService.playerGetServerMails(player.getId());

            //创建 playerController
            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);
            //需要设置workId，否则hall模块所有的请求都将处于多线程环境，当连点发生时不能保证逻辑顺序执行
            session.setWorkId(player.getId());

            if (register[0]) {
                hallService.saveDefaultAvatar(req.playerId);
                hallPlayerService.savePlayerNick(req.playerId, player.getNickName());
            }
            log.info("玩家登录成功 playerId = {},res = {}", player.getId(), JSON.toJSONString(res));

            //我的赌场未保存完成进入新节点 切换到上个节点
            String lastNode = playerBuildingService.getLastNode(player.getId());
            if (StringUtils.isNotEmpty(lastNode)) {
                MarsNode node = clusterSystem.getNode(lastNode);
                if (Objects.nonNull(node)) {
                    clusterSystem.switchNode(playerController.getSession(), node);
                    log.info("我的赌场 切换到上次未保存完的节点");
                } else {
                    log.error("我的赌场信息存在进入未保存完信息的节点");
                }
            }
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            session.send(res);
            log.error("", e);
        }
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
        Player player = hallPlayerService.doSave(playerId, p -> {
            p.setRoomId(0);
            p.setGameType(0);
            p.setRoomCfgId(0);
        });
        PlayerController playerController = new PlayerController(session, player);
        session.setReference(playerController);

        playerSessionService.updateNodePath(session, player);
        log.debug("玩家进入大厅节点 playerId={}", playerId);
    }

    /**
     * 检查是否重连
     *
     * @param player
     * @return
     */
    private boolean reconnect(PFSession session, Player player, PlayerSessionInfo playerSessionInfo) {
        MarsNode node = null;
        //先判断有房间类的游戏重连
        if (player.getRoomId() > 0) {
            //获取该房间数据
            Room room = hallRoomDao.getRoom(player.getGameType(), player.getRoomId());
            if (room == null) {
                log.warn("断线重连时，获取房间对象为空 playerId = {},roomId = {}", player.getId(), player.getRoomId());
                hallPlayerService.doSave(player.getId(), (p) -> {
                    p.setRoomId(0);
                    p.setGameType(0);
                    p.setRoomCfgId(0);
                });
                return false;
            }
            String path = room.getPath();
            //获取房间所在的节点
            node = clusterSystem.getNode(path);
            if (node == null) {
                log.warn("断线重连时，房间所在的节点为空 playerId = {},roomId = {},path = {}",
                    player.getId(), player.getRoomId(), node.getNodePath());
                hallPlayerService.doSave(player.getId(), (p) -> {
                    p.setRoomId(0);
                    p.setGameType(0);
                    p.setRoomCfgId(0);
                });
                return false;
            }
        } else {
            //没有房间类的游戏重连
            Optional<PlayerLastGameInfo> op = playerLastGameInfoDao.findById(player.getId());
            if (op.isEmpty()) {
                return false;
            }

            PlayerLastGameInfo playerLastGameInfo = op.get();
            if (!playerLastGameInfo.isHalfwayOffline() || StringUtils.isEmpty(playerLastGameInfo.getNodePath())) {
                return false;
            }

            //获取节点
            node = clusterSystem.getNode(playerLastGameInfo.getNodePath());
            if (node == null) {
                node =
                    nodeManager.getGameNodeByWeight(playerLastGameInfo.getGameType(), player.getId(), player.getIp());
                if (node == null) {
                    playerLastGameInfo.setHalfwayOffline(false);
                    playerLastGameInfo.setNodePath(null);
                    playerLastGameInfoDao.save(playerLastGameInfo);
                    return false;
                }
            }
        }
        log.info("玩家重连开始切换节点 playerId={},gameType={},toNode = {}",
            player.getId(), player.getGameType(), node.getNodePath());
        playerSessionService.updateReconnectStatus(true, playerSessionInfo);
        clusterSystem.switchNode(session, node);
        return true;
    }

    /**
     * 获取当前跑马灯
     *
     * @return
     */
    private MarqueeInfo addMarquee() {
        try {
            Marquee marquee = marqueeManager.getCurrentMarquee();
            if (marquee == null) {
                return null;
            }

            MarqueeInfo marqueeInfo = marqueeManager.transMarqueeInfo(marquee);
            log.debug("登录获取当前跑马灯 marqueeInfo = {}", JSON.toJSONString(marqueeInfo));
            return marqueeInfo;
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }
}
