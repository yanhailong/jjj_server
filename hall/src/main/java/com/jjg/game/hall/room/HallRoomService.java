package com.jjg.game.hall.room;

import com.jjg.game.common.baselogic.IConsoleReceiver;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.match.MatchDataDao;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.hall.dao.HallRoomDao;
import com.jjg.game.hall.match.MatchService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.Comparator;
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
    @Autowired
    private MatchDataDao matchDataDao;

    public int enterSlotsNode(PlayerController playerController, int roomCfgId) {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg == null) {
            log.error("配置表异常，未在房间表（warehouse.xlsx）中找到房间配置表ID: {}", roomCfgId);
            return Code.SAMPLE_ERROR;
        }
        int gameType = warehouseCfg.getGameID();
        MarsNode marsNode = nodeManager.getGameNodeByWeight(gameType, playerController.playerId(),
                playerController.getPlayer().getIp());

        if (marsNode == null) {
            log.debug("获取游戏节点为空，进入游戏失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return Code.NOT_FOUND;
        }

        playerSessionService.changeGameType(playerController.playerId(), gameType, roomCfgId);
        clusterSystem.switchNode(playerController.getSession(), marsNode);
        return Code.SUCCESS;
    }

    /**
     * 大厅的加入房间
     *
     * @param playerController player控制器
     * @param roomCfgId        大厅房间默认配置ID
     */
    public int hallJoinRoom(PlayerController playerController, int roomCfgId) {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg == null) {
            log.error("加入房间时 配置表异常，未在房间表（warehouse.xlsx）中找到房间配置表ID: {}", roomCfgId);
            return Code.SAMPLE_ERROR;
        }
        int gameType = warehouseCfg.getGameID();
        // 特殊逻辑，百家乐需要将玩家直接传送到游戏服，但是又不进游戏
        if (gameType == EGameType.BACCARAT.getGameTypeId()) {
            // 将玩家切换到某个游戏类型的master游戏服,
            handleBaccaratJoinGame(playerController, roomCfgId);
            // 直接返回成功
            return Code.SUCCESS;
        }
        Pair<MarsNode, Boolean> marsNodeBooleanPair = nodeManager.getGameNodePairByWeight(gameType, playerController.playerId(),
                playerController.getPlayer().getIp());
        if (marsNodeBooleanPair == null) {
            log.debug("加入房间时 获取游戏节点为空，进入游戏失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return Code.NOT_FOUND;
        }
        MarsNode marsNode = marsNodeBooleanPair.getFirst();
        //白名单
        if (marsNodeBooleanPair.getSecond()) {
            List<Room> chooseNodeRoom = new ArrayList<>(hallRoomDao.getChooseNodeRoom(marsNode.getNodePath(), gameType, roomCfgId));
            if (chooseNodeRoom.isEmpty()) {
                //创建一个房间
                long waitingRoomId = createRoom(playerController, roomCfgId, warehouseCfg, gameType, marsNode);
                return joinRoomById(playerController, waitingRoomId, gameType);
            }
            chooseNodeRoom.sort(Comparator.comparingInt(r -> r.getRoomPlayers().size()));
            //随机选取一个
            Room room = chooseNodeRoom.getFirst();
            boolean joinNum = matchDataDao.changeRoomJoinNum(gameType, roomCfgId, room.getId(), room.getMaxLimit(), 1, 1);
            if(!joinNum) {
                //创建一个房间
                long waitingRoomId = createRoom(playerController, roomCfgId, warehouseCfg, gameType, marsNode);
                return joinRoomById(playerController, waitingRoomId, gameType);
            }
            return joinRoomById(playerController, room.getId(), gameType);
        }
        // 获取一个等待房间，如果有空闲的话
        long waitingRoomId = matchService.getWaitingRoomId(playerController, gameType, roomCfgId, SampleDataUtils.getRoomMaxLimit(warehouseCfg).getT2(), marsNode.getNodePath());
        // 如果对应的游戏类型没有房间的话则创建一个新的房间
        if (waitingRoomId == 0) {
            log.error("加入房间时 获取房间为空，进入游戏失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return Code.NOT_FOUND;
        }
        // 加入房间
        return joinRoomById(playerController, waitingRoomId, gameType);
    }

    /**
     * 创建一个房间
     *
     * @param playerController 玩家控制器
     * @param roomCfgId        房间配置id
     * @param warehouseCfg     场次配置
     * @param gameType         游戏类型
     * @param marsNode         节点
     * @return 创建的id
     */
    private long createRoom(PlayerController playerController, int roomCfgId, WarehouseCfg warehouseCfg, int gameType, MarsNode marsNode) {
        long waitingRoomId;
        Tuple2<Integer, Integer> roomMaxLimitCfg = SampleDataUtils.getRoomMaxLimit(warehouseCfg);
        int maxLimit = roomMaxLimitCfg.getT2();
        Room room = hallRoomDao.createRoom(playerController, gameType, roomCfgId, maxLimit, marsNode.getNodePath());
        if (maxLimit != 1) {
            // 如果房间的限制人数不止一个，则将当前房间ID挂到房间等待列表中，等待后续玩家的加入
            matchService.addWaitingRoomId(gameType, roomCfgId, room.getId(), room.getCreateTime());
        }
        waitingRoomId = room.getId();
        return waitingRoomId;
    }

    /**
     * 百家乐玩家进入游戏特殊处理,需要先将玩家传到百家乐游戏类型的主节点上,再获取所有同类型游戏节点的房间摘要信息,当玩家进入某个
     * 节点的游戏时，还需要将当前节点切换到对应的节点上，再开始游戏
     */
    private void handleBaccaratJoinGame(PlayerController playerController, int roomCfgId) {
        // 获取所有的游戏
        MarsNode marsNode =
                nodeManager.getGameNodeByWeight(
                        EGameType.BACCARAT.getGameTypeId(), playerController.playerId(), playerController.getPlayer().getIp());
        //更新session中的gametype
        playerSessionService.changeGameType(
                playerController.playerId(), EGameType.BACCARAT.getGameTypeId(), roomCfgId);
        //切换节点
        clusterSystem.switchNode(playerController.getSession(), marsNode);
    }

    /**
     * 加入好友房房间，好友房特殊点：房间在关服时时间未过期时不会销毁，所以path是动态分配的
     */
    public int joinFriendRoom(PlayerController playerController, long roomId, int gameType) {
        // 查询房间
        Room room = hallRoomDao.getRoom(gameType, roomId);
        if (room == null) {
            log.error("通过ID: {} 找不到好友房房间", roomId);
            return Code.ROOM_NOT_FOUND;
        }
        MarsNode marsNode;
        if (StringUtils.isEmpty(room.getPath())) {
            // 随机分配一个
            marsNode =
                    nodeManager.getGameNodeByWeight(
                            gameType, playerController.playerId(), playerController.getPlayer().getIp());
            if (marsNode == null) {
                log.debug("加入好友房房间时 获取游戏节点为空，进入游戏失败 playerId = {},gameType = {}",
                        playerController.playerId(), gameType);
                return Code.NOT_FOUND;
            }
            updateFriendRoomPath(room, marsNode);
        } else {
            marsNode = marsCurator.getMarsNode(room.getPath());
            // 查询房间节点 TODO 需要判断当前节点是否是维护节点，如果是维护的节点需要重新挑选一个节点
            if (marsNode == null) {
                // 随机分配一个节点
                marsNode = nodeManager.getGameNodeByWeight(
                        gameType, playerController.playerId(), playerController.getPlayer().getIp());
                if (marsNode == null) {
                    // 直接返回错误
                    return Code.FAIL;
                }
                updateFriendRoomPath(room, marsNode);
            }
        }
        // 更新玩家的房间ID
        playerController.setPlayer(
                playerService.doSave(playerController.playerId(), (player) -> player.setRoomId(room.getId())));
        //更新session中的gametype
        playerSessionService.changeGameType(playerController.playerId(), gameType, room.getRoomCfgId());
        //切换节点
        clusterSystem.switchNode(playerController.getSession(), marsNode);
        return Code.SUCCESS;
    }

    /**
     * 更新房间节点路径
     */
    private void updateFriendRoomPath(Room room, MarsNode marsCurator) {
        hallRoomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(Room dataEntity) {

            }

            @Override
            public boolean updateDataWithRes(Room dataEntity) {
                // 需要将房间的节点路径进行更新
                dataEntity.setPath(marsCurator.getNodePath());
                return true;
            }
        });
    }

    /**
     * 通过房间ID加入房间
     */
    private int joinRoomById(PlayerController playerController, long roomId, int gameType) {
        // 查询房间
        Room room = hallRoomDao.getRoom(gameType, roomId);
        if (room == null) {
            log.error("通过ID: {} 找不到房间", roomId);
            return Code.ROOM_NOT_FOUND;
        }
        // 查询房间节点
        MarsNode marsNode = marsCurator.getMarsNode(room.getPath());
        // marsNode为空，可能服务器断掉也可能是服务器关闭，但是服务器关闭房间还存在
        // 可能房间还存在，也可能房间不存在，不能确定，但是不能直接将房间删除，应该将异常房间ID放在WaitJoinRoomId的最开始
        // 让清理异常的房间主节点去判断是否应该删除
        if (marsNode == null) {
            log.error("房间: {} 对应的节点: {} 不存在或者已关闭", room.getId(), room.getPath());
            // 将有问题的房间ID移动到最前面，房间主节点去处理异常的房间
            matchDataDao.removeWaitJoinRoomId(gameType, room.getRoomCfgId(), roomId);
            // 直接返回错误
            return Code.FAIL;
        } else {
            // TODO 需要判断当前节点是否是维护节点，如果是维护的节点需要重新挑选一个节点
        }
        //等待
        matchService.addPlayerExpiredWaiting(roomId, playerController.playerId());
        // 更新玩家的房间ID
        playerController.setPlayer(
                playerService.doSave(playerController.playerId(), (player) -> player.setRoomId(room.getId())));
        //更新session中的gametype
        playerSessionService.changeGameType(playerController.playerId(), gameType, room.getRoomCfgId());
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
