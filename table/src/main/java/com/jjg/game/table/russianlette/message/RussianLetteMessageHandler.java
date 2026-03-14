package com.jjg.game.table.russianlette.message;

import com.jjg.game.common.baselogic.IConsoleReceiver;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.*;
import com.jjg.game.core.match.MatchDataDao;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.dao.RoomDao;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.table.baccarat.BaccaratTempRoom;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.russianlette.RussianLetteGameController;
import com.jjg.game.table.russianlette.RussianLetteTempRoom;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.message.req.ReqRussianLetteExitRoomInGame;
import com.jjg.game.table.russianlette.message.req.ReqRussianLetteInfo;
import com.jjg.game.table.russianlette.message.req.ReqRussianLetteJoinRoomInGame;
import com.jjg.game.table.russianlette.message.req.ReqRussianLetteOtherSummaryList;
import com.jjg.game.table.russianlette.message.req.ReqRussianLetteSummary;
import com.jjg.game.table.russianlette.message.req.ReqRussianLetteSummaryList;
import com.jjg.game.table.russianlette.message.req.ReqRussianLetteSwitchRoomInGame;
import com.jjg.game.table.russianlette.message.resp.RespRussianLetteExitRoomInGame;
import com.jjg.game.table.russianlette.message.resp.RespRussianLetteInfo;
import com.jjg.game.table.russianlette.message.resp.RespRussianLetteJoinRoomInGame;
import com.jjg.game.table.russianlette.message.resp.RespRussianLetteOtherSummaryList;
import com.jjg.game.table.russianlette.message.resp.RespRussianLetteSummary;
import com.jjg.game.table.russianlette.message.resp.RespRussianLetteSummaryList;
import com.jjg.game.table.russianlette.message.resp.RespRussianLetteSwitchRoomInGame;
import com.jjg.game.table.russianlette.message.resp.RussianLetteSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 俄罗斯轮盘消息处理器
 * <p>
 * 负责处理客户端发来的以下请求：
 * <ul>
 *   <li>{@code REQ_RUSSIAN_LETTE_INFO}               — 请求当前房间桌面信息（断线重连 / 首次进入）</li>
 *   <li>{@code REQ_RUSSIAN_LETTE_SUMMARY_LIST}        — 请求同场次所有房间摘要列表</li>
 *   <li>{@code REQ_RUSSIAN_LETTE_SUMMARY}             — 请求单个房间摘要（按 roomId）</li>
 *   <li>{@code REQ_RUSSIAN_LETTE_OTHER_SUMMARY_LIST}  — 请求其他场次所有房间摘要列表</li>
 *   <li>{@code REQ_JOIN_ROOM_IN_GAME}                 — 在游戏中请求进入指定房间</li>
 *   <li>{@code REQ_SWITCH_ROOM_IN_GAME}               — 在游戏中请求切换到另一个房间</li>
 *   <li>{@code REQ_EXIT_ROOM_IN_GAME}                 — 请求退出当前房间</li>
 * </ul>
 *
 * @author 2CL / lhc
 */
@MessageType(MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE)
@ProtoDesc("俄罗斯轮盘handler")
@Component
public class RussianLetteMessageHandler implements IConsoleReceiver {

    private final Logger log = LoggerFactory.getLogger(RussianLetteMessageHandler.class);
    private final RoomManager roomManager;
    private final NodeManager nodeManager;
    private final ClusterSystem clusterSystem;
    private final RoomDao roomDao;
    private final CorePlayerService playerService;
    private final RussianLetteTempRoom russianLetteTempRoom;
    private final MatchDataDao matchDataDao;

    public RussianLetteMessageHandler(RoomManager roomManager, NodeManager nodeManager,
                                      ClusterSystem clusterSystem, RoomDao roomDao,
                                      CorePlayerService playerService, MatchDataDao matchDataDao,
                                      RussianLetteTempRoom russianLetteTempRoom) {
        this.roomManager = roomManager;
        this.nodeManager = nodeManager;
        this.clusterSystem = clusterSystem;
        this.roomDao = roomDao;
        this.playerService = playerService;
        this.russianLetteTempRoom = russianLetteTempRoom;
        this.matchDataDao = matchDataDao;
    }

    // =========================================================================
    // 请求处理
    // =========================================================================

    /**
     * 请求俄罗斯转盘房间桌面信息
     * <p>玩家进入房间后 / 断线重连时调用，服务端推送 {@code NotifyRussianLetteTableInfo}。</p>
     */
    @Command(RussianLetteMessageConstant.ReqMsgBean.REQ_RUSSIAN_LETTE_INFO)
    public void reqRussianLetteInfo(PlayerController playerController, ReqRussianLetteInfo req) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController == null) {
            log.error("reqRussianLetteInfo: 玩家 {} 找不到对应的游戏控制器", playerController.playerId());
            playerController.send(new RespRussianLetteInfo(Code.FAIL));
            return;
        }
        if (gameController.gameControlType() != EGameType.RUSSIAN_ROULETTE) {
            log.error("reqRussianLetteInfo: 玩家 {} 当前不在俄罗斯转盘游戏中", playerController.playerId());
            playerController.send(new RespRussianLetteInfo(Code.PARAM_ERROR));
            return;
        }
        // 构建并发送完整的桌面信息响应（含所有字段）
        RussianLetteGameController rgc = (RussianLetteGameController) gameController;
        RespRussianLetteInfo resp = RussianLetteMessageBuilder.buildRespRussianLetteInfo(
                playerController.playerId(), rgc);
        playerController.send(resp);
        // 更新玩家操作时间（心跳续约）
        TableGameDataVo tableGameDataVo = (TableGameDataVo) gameController.getGameDataVo();
        tableGameDataVo.updatePlayerOperateTime(playerController.playerId());
    }

    /**
     * 请求同场次所有房间的摘要列表
     * <p>大厅选场次界面使用，返回玩家当前场次（wareId）下的所有运行中房间概要。</p>
     */
    @Command(RussianLetteMessageConstant.ReqMsgBean.REQ_RUSSIAN_LETTE_SUMMARY_LIST)
    public void reqRussianLetteSummaryList(PlayerController playerController, ReqRussianLetteSummaryList req) {
        int roomCfgId = playerController.getPlayer().getRoomCfgId();
        CommonResult<List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> result =
                getRussianLetteGameControllers(roomCfgId);
        if (result.code != Code.SUCCESS) {
            playerController.send(new RespRussianLetteSummaryList(result.code));
            return;
        }
        RespRussianLetteSummaryList respList = new RespRussianLetteSummaryList(Code.SUCCESS);
        respList.tableSummaryList = new ArrayList<>();
        for (AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gc : result.data) {
            if (gc instanceof RussianLetteGameController russianLetteGc) {
                RussianLetteSummary summary = RussianLetteMessageBuilder.buildRussianLetteSummaryInfo(russianLetteGc);
                respList.tableSummaryList.add(summary);
            }
        }
        playerController.send(respList);
    }

    /**
     * 请求单个房间摘要信息
     * <p>客户端刷新指定房间的实时状态时使用，按 {@code req.roomId} 定位具体房间。</p>
     */
    @Command(RussianLetteMessageConstant.ReqMsgBean.REQ_RUSSIAN_LETTE_SUMMARY)
    public void reqRussianLetteSummary(PlayerController playerController, ReqRussianLetteSummary req) {
        List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> gameControllers =
                roomManager.getGameControllersByGameType(EGameType.RUSSIAN_ROULETTE, RoomType.BET_ROOM);
        if (gameControllers == null || gameControllers.isEmpty()) {
            playerController.send(new RespRussianLetteSummary(Code.FAIL));
            return;
        }
        for (AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gc : gameControllers) {
            if (gc instanceof RussianLetteGameController rgc
                    && rgc.getGameDataVo().getRoomId() == req.roomId) {
                RespRussianLetteSummary resp = new RespRussianLetteSummary(Code.SUCCESS);
                resp.summary = RussianLetteMessageBuilder.buildRussianLetteSummary(rgc);
                playerController.send(resp);
                // 进入房间需要先将玩家从临时房间中移除
                PlayerSessionInfo playerSessionInfo = new PlayerSessionInfo();
                playerSessionInfo.setRoomCfgId(gc.getRoom().getRoomCfgId());
                russianLetteTempRoom.enter(playerController.getSession(), playerController, playerSessionInfo);
                return;
            }
        }
        log.warn("reqRussianLetteSummary: 找不到 roomId={} 的房间", req.roomId);

        playerController.send(new RespRussianLetteSummary(Code.ROOM_NOT_FOUND));
    }

    /**
     * 请求其他场次的房间摘要列表
     * <p>用于跨场次切换 UI，返回与当前玩家场次（roomCfgId）不同的所有俄罗斯转盘房间摘要。</p>
     */
    @Command(RussianLetteMessageConstant.ReqMsgBean.REQ_RUSSIAN_LETTE_OTHER_SUMMARY_LIST)
    public void reqRussianLetteOtherSummaryList(PlayerController playerController, ReqRussianLetteOtherSummaryList req) {
        // 当前玩家所在场次
        int currentRoomCfgId = playerController.getPlayer().getRoomCfgId();
        List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> gameControllers =
                roomManager.getGameControllersByGameType(EGameType.RUSSIAN_ROULETTE, RoomType.BET_ROOM);
        RespRussianLetteOtherSummaryList resp = new RespRussianLetteOtherSummaryList(Code.SUCCESS);
        resp.tableSummaryList = new ArrayList<>();
        if (gameControllers != null && !gameControllers.isEmpty()) {
            for (AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gc : gameControllers) {
                // 只返回其他场次（roomCfgId 不同）的房间
                if (gc instanceof RussianLetteGameController rgc
                        && gc.getRoom().getRoomCfgId() == currentRoomCfgId) {
                    resp.tableSummaryList.add(RussianLetteMessageBuilder.buildRussianLetteSummaryInfo(rgc));
                }
            }
        }
        playerController.send(resp);
    }

    /**
     * 在游戏中请求进入指定俄罗斯转盘房间
     * <p>
     * 流程：校验游戏类型 → 查询房间 → 锁定等待人数 → 同节点直接入房 / 跨节点切换节点。
     * </p>
     */
    @Command(value = RussianLetteMessageConstant.ReqMsgBean.REQ_JOIN_ROOM_IN_GAME)
    public void joinRoomInGame(PlayerController playerController, ReqRussianLetteJoinRoomInGame req) {
        // 只允许通过俄罗斯转盘消息进入俄罗斯转盘房间
        if (req.gameType != EGameType.RUSSIAN_ROULETTE.getGameTypeId()) {
            log.error("joinRoomInGame: 玩家 {} 请求的 gameType={} 非俄罗斯转盘", playerController.playerId(), req.gameType);
            playerController.send(new RespRussianLetteJoinRoomInGame(Code.PARAM_ERROR));
            return;
        }
        // 查询目标房间
        Room room = roomDao.getRoom(req.gameType, req.roomId);
        if (room == null) {
            log.error("joinRoomInGame: roomId={} 的房间不存在或已销毁", req.roomId);
            playerController.send(new RespRussianLetteJoinRoomInGame(Code.ROOM_NOT_FOUND));
            return;
        }
        RespRussianLetteJoinRoomInGame resp = new RespRussianLetteJoinRoomInGame(Code.SUCCESS);
        resp.roomCfgId = room.getRoomCfgId();
        // 等待进入人数 +1（含满员检测）
        boolean joinOk = matchDataDao.changeRoomJoinNum(
                req.gameType, room.getRoomCfgId(), room.getId(), room.getMaxLimit(), 1, 1);
        if (!joinOk) {
            resp.code = Code.ROOM_FULL;
            playerController.send(resp);
            return;
        }
        // 进入房间需要先将玩家从临时房间中移除
        russianLetteTempRoom.exit(playerController.getSession(), playerController);
        // 将玩家加入过期等待列表，防止入房超时
        matchDataDao.addPlayerExpiredWaiting(req.roomId, playerController.playerId());
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> currentGc =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (currentGc != null && currentGc.gameControlType() == EGameType.RUSSIAN_ROULETTE) {
            currentGc.getGamePlayer(playerController.playerId()).getTableGameData().setPlayNum(0);
        }
        String localNodePath = clusterSystem.getNodePath();
        if (localNodePath.equalsIgnoreCase(room.getPath())) {
            // ── 同节点：直接加入房间 ──────────────────────────────────────────
            int result = roomManager.joinRoom(playerController, req.gameType, room.getRoomCfgId(), req.roomId);
            log.info("joinRoomInGame: 玩家 {} 同节点加入俄罗斯转盘房间 {} result={}",
                    playerController.playerId(), req.roomId, result);
            if (result != Code.SUCCESS) {
                // 入房失败，回滚等待人数
                boolean rollback = matchDataDao.changeRoomJoinNum(
                        req.gameType, room.getRoomCfgId(), room.getId(), room.getMaxLimit(), -1, -1);
                if (!rollback) {
                    log.error("joinRoomInGame 回滚等待人数失败 playerId:{} roomCfgId:{} roomId:{}",
                            playerController.playerId(), room.getRoomCfgId(), room.getId());
                }
            } else {
                // 绑定 session workId 到新房间
                playerController.getSession().setWorkId(req.roomId);
            }
            resp.code = result;
            playerController.send(resp);
        } else {
            // ── 跨节点：切换节点处理 ──────────────────────────────────────────
            MarsNode marsNode = nodeManager.getMarNode(room.getPath());
            if (marsNode == null) {
                boolean rollback = matchDataDao.changeRoomJoinNum(
                        req.gameType, room.getRoomCfgId(), room.getId(), room.getMaxLimit(), -1, -1);
                if (!rollback) {
                    log.error("joinRoomInGame 跨节点回滚等待人数失败 playerId:{} roomCfgId:{} roomId:{}",
                            playerController.playerId(), room.getRoomCfgId(), room.getId());
                }
                resp.code = Code.FAIL;
                playerController.send(resp);
                return;
            }
            // 持久化 roomId，sessionEnter 时自动完成入房
            playerService.doSave(playerController.playerId(), (player) -> player.setRoomId(req.roomId));
            clusterSystem.switchNode(playerController.getSession(), marsNode);
            log.info("joinRoomInGame: 玩家 {} 跨节点加入俄罗斯转盘房间 {} 目标节点:{}",
                    playerController.playerId(), req.roomId, room.getPath());
            playerController.send(resp);
        }
    }

    /**
     * 在游戏中请求切换到另一个俄罗斯转盘房间
     * <p>
     * 流程：先退出当前房间 → 查询目标房间 → 锁定等待人数 → 同节点直接入房 / 跨节点切换节点。
     * </p>
     */
    @Command(value = RussianLetteMessageConstant.ReqMsgBean.REQ_SWITCH_ROOM_IN_GAME)
    public void switchRoomInGame(PlayerController playerController, ReqRussianLetteSwitchRoomInGame req) {
        int gameTypeId = EGameType.RUSSIAN_ROULETTE.getGameTypeId();
        // 查询目标房间
        Room room = roomDao.getRoom(gameTypeId, req.roomId);
        if (room == null) {
            log.error("switchRoomInGame: 目标 roomId={} 不存在或已销毁", req.roomId);
            playerController.send(new RespRussianLetteSwitchRoomInGame(Code.ROOM_NOT_FOUND));
            return;
        }
        // 先退出当前房间（若当前在俄罗斯转盘中）
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> currentGc =
                roomManager.getGameControllerByPlayerId(playerController.playerId());

        if (currentGc != null && currentGc.gameControlType() == EGameType.RUSSIAN_ROULETTE) {
            if(currentGc instanceof RussianLetteGameController rgc) {
                if(rgc.getCurrentGamePhase() != EGamePhase.BET){
                    playerController.send(new RespRussianLetteSwitchRoomInGame(Code.FORBID));
                    return;
                }
            }
            currentGc.getGamePlayer(playerController.playerId()).getTableGameData().setPlayNum(0);
            int exitCode = roomManager.exitRoom(playerController, false);
            log.info("switchRoomInGame: 玩家 {} 退出当前房间 exitCode={}", playerController.playerId(), exitCode);
        }

        RespRussianLetteSwitchRoomInGame resp = new RespRussianLetteSwitchRoomInGame(Code.SUCCESS);
        resp.roomCfgId = room.getRoomCfgId();
        // 等待进入人数 +1
        boolean joinOk = matchDataDao.changeRoomJoinNum(
                gameTypeId, room.getRoomCfgId(), room.getId(), room.getMaxLimit(), 1, 1);
        if (!joinOk) {
            resp.code = Code.ROOM_FULL;
            playerController.send(resp);
            return;
        }
        // 进入房间需要先将玩家从临时房间中移除
        russianLetteTempRoom.exit(playerController.getSession(), playerController);
        matchDataDao.addPlayerExpiredWaiting(req.roomId, playerController.playerId());
        String localNodePath = clusterSystem.getNodePath();
        if (localNodePath.equalsIgnoreCase(room.getPath())) {
            // ── 同节点：直接加入房间 ──────────────────────────────────────────
            int result = roomManager.joinRoom(playerController, gameTypeId, room.getRoomCfgId(), req.roomId);
            log.info("switchRoomInGame: 玩家 {} 同节点切换到房间 {} result={}",
                    playerController.playerId(), req.roomId, result);
            if (result != Code.SUCCESS) {
                boolean rollback = matchDataDao.changeRoomJoinNum(
                        gameTypeId, room.getRoomCfgId(), room.getId(), room.getMaxLimit(), -1, -1);
                if (!rollback) {
                    log.error("switchRoomInGame 回滚等待人数失败 playerId:{} roomCfgId:{} roomId:{}",
                            playerController.playerId(), room.getRoomCfgId(), room.getId());
                }
            } else {
                playerController.getSession().setWorkId(req.roomId);
            }
            resp.code = result;
            playerController.send(resp);
        } else {
            // ── 跨节点：切换节点处理 ──────────────────────────────────────────
            MarsNode marsNode = nodeManager.getMarNode(room.getPath());
            if (marsNode == null) {
                boolean rollback = matchDataDao.changeRoomJoinNum(
                        gameTypeId, room.getRoomCfgId(), room.getId(), room.getMaxLimit(), -1, -1);
                if (!rollback) {
                    log.error("switchRoomInGame 跨节点回滚等待人数失败 playerId:{} roomCfgId:{} roomId:{}",
                            playerController.playerId(), room.getRoomCfgId(), room.getId());
                }
                resp.code = Code.FAIL;
                playerController.send(resp);
                return;
            }
            playerService.doSave(playerController.playerId(), (player) -> player.setRoomId(req.roomId));
            clusterSystem.switchNode(playerController.getSession(), marsNode);
            log.info("switchRoomInGame: 玩家 {} 跨节点切换到房间 {} 目标节点:{}",
                    playerController.playerId(), req.roomId, room.getPath());
            playerController.send(resp);
        }
    }

    /**
     * 请求退出当前俄罗斯转盘房间
     * <p>
     * 玩家主动离开房间时调用。退出成功后将玩家加入临时房间（观察者），
     * 并主动推送当前场次的所有房间摘要，供客户端选房界面展示。
     * </p>
     */
    @Command(value = RussianLetteMessageConstant.ReqMsgBean.REQ_EXIT_ROOM_IN_GAME)
    public void exitRoomInGame(PlayerController playerController, ReqRussianLetteExitRoomInGame req) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController == null) {
            log.error("exitRoomInGame: 玩家 {} 找不到对应的游戏控制器", playerController.playerId());
            playerController.send(new RespRussianLetteExitRoomInGame(Code.FAIL));
            return;
        }
        if (gameController.gameControlType() != EGameType.RUSSIAN_ROULETTE) {
            log.error("exitRoomInGame: 玩家 {} 当前不在俄罗斯转盘游戏中", playerController.playerId());
            playerController.send(new RespRussianLetteExitRoomInGame(Code.PARAM_ERROR));
            return;
        }

        // 记录场次 ID，退出后用于加入临时房间和发送摘要
        int roomCfgId = gameController.getRoom().getRoomCfgId();

        gameController.getGamePlayer(playerController.playerId()).getTableGameData().setPlayNum(0);
        int code = roomManager.exitRoom(playerController, false);
        playerController.send(new RespRussianLetteExitRoomInGame(code));
        log.info("exitRoomInGame: 玩家 {} 退出俄罗斯转盘房间 code={}", playerController.playerId(), code);

        if (code == Code.SUCCESS) {
            // 加入临时房间（观察者），后续阶段变化时会收到 NotifyRussianLetteTableSummary 推送
            PlayerSessionInfo playerSessionInfo = new PlayerSessionInfo();
            playerSessionInfo.setRoomCfgId(roomCfgId);
            russianLetteTempRoom.enter(playerController.getSession(), playerController, playerSessionInfo);

            // 主动推送当前场次所有房间的摘要列表，供选房界面立即展示
            CommonResult<List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> result =
                    getRussianLetteGameControllers(roomCfgId);
            if (result.code == Code.SUCCESS) {
                RespRussianLetteSummaryList respList = new RespRussianLetteSummaryList(Code.SUCCESS);
                respList.tableSummaryList = new ArrayList<>();
                for (AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gc : result.data) {
                    if (gc instanceof RussianLetteGameController russianLetteGc) {
                        respList.tableSummaryList.add(
                                RussianLetteMessageBuilder.buildRussianLetteSummaryInfo(russianLetteGc));
                    }
                }
                playerController.send(respList);
            }
        }
    }

    // =========================================================================
    // 内部工具
    // =========================================================================

    /**
     * 获取指定场次配置下所有俄罗斯转盘房间的游戏控制器列表
     *
     * @param roomCfgId 场次配置 ID
     * @return 包含控制器列表的结果；若不存在则返回 FAIL
     */
    private CommonResult<List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> getRussianLetteGameControllers(int roomCfgId) {
        List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> gameControllers =
                roomManager.getGameControllersByGameType(EGameType.RUSSIAN_ROULETTE, RoomType.BET_ROOM);
        if (gameControllers == null || gameControllers.isEmpty()) {
            log.warn("getRussianLetteGameControllers: 未找到俄罗斯转盘房间 roomCfgId:{}", roomCfgId);
            return new CommonResult<>(Code.FAIL);
        }
        List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> filtered =
                gameControllers.stream()
                        .filter(gc -> gc.getRoom().getRoomCfgId() == roomCfgId)
                        .toList();
        if (filtered.isEmpty()) {
            return new CommonResult<>(Code.FAIL);
        }
        return new CommonResult<>(Code.SUCCESS, filtered);
    }

    // =========================================================================
    // 控制台命令（运维调试）
    // =========================================================================

    @Override
    public void doCommand(String command, List<String> params) {
        switch (command) {
            case "summaryList":
                log.info("[RussianLette] 控制台命令 summaryList, params={}", params);
                break;
            default:
                break;
        }
    }

    @Override
    public List<String> needHandleCommands() {
        return List.of("summaryList");
    }
}
