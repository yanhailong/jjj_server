package com.jjg.game.hall.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.tool.IConsoleReceiver;
import com.jjg.game.hall.dao.HallRoomDao;
import com.jjg.game.hall.match.MatchService;
import com.jjg.game.hall.sample.GameDataManager;
import com.jjg.game.hall.sample.bean.WarehouseCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 大厅的房间处理
 *
 * @author 2CL
 */
@Service
public class HallRoomService implements IConsoleReceiver {

    private static final Logger log = LoggerFactory.getLogger(HallRoomService.class);
    @Autowired
    private HallRoomDao hallRoomDao;
    @Autowired
    private MatchService matchService;
    @Autowired
    ClusterSystem clusterSystem;
    @Autowired
    PlayerSessionService playerSessionService;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private MarsCurator marsCurator;

    /**
     * 大厅的加入房间
     *
     * @param playerController player控制器
     * @param roomCfgId        大厅房间默认配置ID
     */
    public int hallJoinRoom(PlayerController playerController, int roomCfgId, int wareId) {
        // 处理玩家重复加入房间的问题
        if (playerController.getPlayer().getRoomId() > 0) {
            int code = dealPlayerRepeatJoin(playerController, playerController.getPlayer().getRoomId());
            if (code != Code.SUCCESS) {
                return code;
            }
        }
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg == null) {
            log.error("配置表异常，未在房间表（warehouse.xlsx）中找到房间配置表ID: {}", roomCfgId);
            return Code.SAMPLE_ERROR;
        }
        int gameType = warehouseCfg.getGameID();
        MarsNode marsNode = nodeManager.getGameNodeByWeight(gameType, playerController.playerId(),
            playerController.getPlayer().getIp());
        // 特殊逻辑，百家乐需要将玩家直接传送到游戏服，但是又不进游戏
        if (gameType == EGameType.BACCARAT.getGameTypeId()) {
            // 将玩家切换到某个游戏类型的master游戏服,
            handleBaccaratJoinGame(playerController, wareId);
            // 直接返回成功
            return Code.SUCCESS;
        }
        if (marsNode == null) {
            log.debug("获取游戏节点为空，进入游戏失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return Code.NOT_FOUND;
        }
        // 获取一个等待房间，如果有空闲的话
        long waitingRoomId = matchService.getWaitingRoomId(gameType, roomCfgId);
        // 如果对应的游戏类型没有房间的话则创建一个新的房间
        if (waitingRoomId == 0) {
            int maxLimit = getRoomMaxLimit(warehouseCfg);
            Room room = hallRoomDao.createRoom(playerController.playerId(), gameType, maxLimit, marsNode.getNodePath());
            room.setRoomCfgId(roomCfgId);
            hallRoomDao.saveRoom(room);
            if (maxLimit != 1) {
                // 如果房间的限制人数不止一个，则将当前房间ID挂到房间等待列表中，等待后续玩家的加入
                matchService.addWaitingRoomId(gameType, roomCfgId, room.getId(), room.getCreateTime());
            }
            waitingRoomId = room.getId();
        }
        // 加入房间
        return joinRoomById(playerController, waitingRoomId, gameType, wareId);
    }

    /**
     * 百家乐玩家进入游戏特殊处理,需要先将玩家传到百家乐游戏类型的主节点上,再获取所有同类型游戏节点的房间摘要信息,当玩家进入某个
     * 节点的游戏时，还需要将当前节点切换到对应的节点上，再开始游戏
     */
    private void handleBaccaratJoinGame(PlayerController playerController, int wareId) {
        // 获取所有的游戏
        MarsNode marsNode = nodeManager.getGameNodeByWeight(EGameType.BACCARAT.getGameTypeId(),
            playerController.playerId(),
            playerController.getPlayer().getIp());
        //更新session中的gametype
        playerSessionService.
            changeGameType(playerController.playerId(), EGameType.BACCARAT.getGameTypeId(), -1, wareId);
        //切换节点
        clusterSystem.switchNode(playerController.getSession(), marsNode);
    }

    /**
     * 通过房间配置获取最大限制
     */
    private int getRoomMaxLimit(WarehouseCfg warehouseCfg) {
        String participantsMax = warehouseCfg.getParticipants_max();
        String[] participantsMaxStrArr = participantsMax.split(":");
        return Integer.parseInt(participantsMaxStrArr[1]);
    }

    /**
     * 玩家通过房间ID加入房间，需要检查加入房间的前置条件
     */
    public int joinRoomByRoomId(PlayerController playerController, long roomId, int gameType, int wareId) {
        // 玩家不能重复加入房间
        if (playerController.getPlayer().getRoomId() > 0) {
            dealPlayerRepeatJoin(playerController, roomId);
            return Code.REPEAT_OP;
        }
        // 加入房间
        return joinRoomById(playerController, roomId, gameType, wareId);
    }

    /**
     * 通过房间ID加入房间
     */
    private int joinRoomById(PlayerController playerController, long roomId, int gameType, int wareId) {
        // 查询房间
        Room room = hallRoomDao.getRoom(gameType, roomId);
        if (room == null) {
            log.error("通过ID: {} 找不到房间", roomId);
            return Code.ROOM_NOT_FOUND;
        }
        // 查询房间节点
        MarsNode marsNode = marsCurator.getMarsNode(room.getPath());
        // 更新玩家的房间ID
        playerController.setPlayer(
            playerService.doSave(playerController.playerId(), (player) -> player.setRoomId(room.getId())));
        //更新session中的gametype
        playerSessionService.changeGameType(playerController.playerId(), gameType, room.getRoomCfgId(), wareId);
        //切换节点
        clusterSystem.switchNode(playerController.getSession(), marsNode);
        return Code.SUCCESS;
    }

    /**
     * 处理玩家重复加入的情况,异常情况,按正常流程不应该出现
     */
    public int dealPlayerRepeatJoin(PlayerController playerController, long roomId) {
        log.warn("玩家：{} 重复请求 加入房间：{}", playerController.playerId(), roomId);
        // TODO 添加处理逻辑
        return Code.FAIL;
    }

    @Override
    public void doCommand(String command, List<String> params) {
        switch (command) {
            case "joinGame":
                //handleBaccaratJoinGame(EGameType.BACCARAT, null);
                break;
            default:
                break;
        }
    }

    @Override
    public List<String> needHandleCommands() {
        return List.of("joinGame");
    }
}
