package com.jjg.game.table.baccarat.message;

import com.jjg.game.common.baselogic.IConsoleReceiver;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.utils.NetUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.dao.RoomDao;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.table.baccarat.BaccaratGameController;
import com.jjg.game.table.baccarat.BaccaratTempRoom;
import com.jjg.game.table.baccarat.message.req.ReqExitRoomInGame;
import com.jjg.game.table.baccarat.message.req.ReqJoinRoomInGame;
import com.jjg.game.table.baccarat.message.resp.*;
import com.jjg.game.table.common.data.TableGameDataVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 百家乐消息handler
 *
 * @author 2CL
 */
@MessageType(MessageConst.MessageTypeDef.BACCARAT_TYPE)
@ProtoDesc("百家乐消息handler")
@Component
public class BaccaratMessageHandler implements IConsoleReceiver {

    private static final Logger log = LoggerFactory.getLogger(BaccaratMessageHandler.class);
    @Autowired
    protected RoomManager roomManager;
    @Autowired
    protected NodeManager nodeManager;
    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    private RoomDao roomDao;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private BaccaratTempRoom baccaratTempRoom;

    /**
     * 请求百家乐房间信息，玩家进入房间时拉取此数据
     */
    @Command(BaccaratMessageConstant.ReqMsgBean.REQ_BACCARAT_TABLE_INFO)
    public void reqBaccaratTableInfo(PlayerController playerController) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
            roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController == null) {
            log.error("玩家： {} 找不到对应的房间", playerController.playerId());
            playerController.send(new RespBaccaratTableInfo(Code.FAIL));
            return;
        }
        // 玩家不在百家乐游戏
        if (gameController.gameControlType() != EGameType.BACCARAT) {
            log.error("玩家： {} 不在百家乐游戏中", playerController.playerId());
            playerController.send(new RespBaccaratTableInfo(Code.PARAM_ERROR));
            return;
        }
        gameController.respRoomInitInfo(playerController);
        TableGameDataVo tableGameDataVo = (TableGameDataVo) gameController.getGameDataVo();
        // 更新操作时间
        tableGameDataVo.updatePlayerOperateTime(playerController.playerId());
    }

    @Command(BaccaratMessageConstant.ReqMsgBean.REQ_BACCARAT_TABLE_SUMMARY_LIST)
    public void reqBaccaratSummaryList(PlayerController playerController) {
        // 获取进入房间时的配置ID
        int roomCfgId = playerController.getPlayer().getRoomCfgId();
        CommonResult<List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> result =
            getBaccaratGameController(roomCfgId);
        if (result.code != Code.SUCCESS) {
            playerController.send(new RespBaccaratTableSummaryList(result.code));
            return;
        }
        List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> gameControllers =
            result.data;
        // 向客户端发送摘要信息
        RespBaccaratTableSummaryList respSummaryList = new RespBaccaratTableSummaryList(Code.SUCCESS);
        respSummaryList.tableSummaryList = new ArrayList<>();
        for (AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameWareController :
            gameControllers) {
            if (gameWareController instanceof BaccaratGameController baccaratGameController) {
                BaccaratTableSummary baccaratTableSummary =
                    BaccaratMessageBuilder.buildBaccaratSummaryInfo(baccaratGameController);
                respSummaryList.tableSummaryList.add(baccaratTableSummary);
            }
        }
        playerController.send(respSummaryList);
    }

    // 获取所有节点并发送数据到对应的节点上
    public void getAllGameNode() {
        String localIpAddress = NetUtils.getLocalIpAddress();
        List<MarsNode> gameNodeList = nodeManager.getGameNodeList(EGameType.BACCARAT.getGameTypeId(), 0,
            localIpAddress);
        try {
            for (MarsNode marsNode : gameNodeList) {
                ClusterClient clusterClient = clusterSystem.getClusterByPath(marsNode.getNodePath());
                if (clusterClient == null) {
                    continue;
                }
                NettyConnect<Object> connect = clusterClient.getConnect();
                log.info(connect.toString());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Command(value = BaccaratMessageConstant.ReqMsgBean.REQ_JOIN_ROOM_IN_GAME)
    public void joinRoomInGame(PlayerController playerController, ReqJoinRoomInGame reqJoinRoomInGame) {
        // 非法的GameType
        if (EGameType.getGameByTypeId(reqJoinRoomInGame.gameType) == null) {
            RespJoinRoomInGame respJoinRoomInGame = new RespJoinRoomInGame(Code.PARAM_ERROR);
            playerController.send(respJoinRoomInGame);
            return;
        }
        Room room = roomDao.getRoom(reqJoinRoomInGame.gameType, reqJoinRoomInGame.roomId);
        if (room == null) {
            // 房间已经销毁或者解散
            RespJoinRoomInGame respJoinRoomInGame = new RespJoinRoomInGame(Code.PARAM_ERROR);
            playerController.send(respJoinRoomInGame);
            return;
        }
        // 发送进入成功消息
        RespJoinRoomInGame respJoinRoomInGame = new RespJoinRoomInGame(Code.SUCCESS);
        respJoinRoomInGame.roomCfgId = room.getRoomCfgId();
        playerController.send(respJoinRoomInGame);
        // 获取当前节点
        String clusterCurrentNodePath = clusterSystem.getNodePath();
        // 进入房间需要先将玩家从临时房间中移除
        baccaratTempRoom.exit(playerController.getSession(), playerController);
        // 如果就在当前节点
        if (clusterCurrentNodePath.equalsIgnoreCase(room.getPath())) {
            // 将玩家加入房间
            roomManager.joinRoom(
                playerController, reqJoinRoomInGame.gameType, room.getRoomCfgId(), reqJoinRoomInGame.roomId);
            log.info("玩家：{} 请求加入房间：{} {} 处于当前节点", playerController.playerId(), room.getRoomCfgId(), room.getId());
        } else {
            // 将玩家的房间ID设置成请求的，在session进入时会自动加入到对应的房间
            playerService.doSave(playerController.playerId(), (player) -> player.setRoomId(reqJoinRoomInGame.roomId));
            // 将玩家切入到对应的房间节点
            MarsNode marsNode = nodeManager.getMarNode(room.getPath());
            // sessionEnter时处理
            //切换节点
            clusterSystem.switchNode(playerController.getSession(), marsNode);
            log.info("玩家：{} 请求加入房间：{} 处于节点：{}", playerController.playerId(), room.getRoomCfgId(), room.getPath());
        }
    }

    @Command(value = BaccaratMessageConstant.ReqMsgBean.REQ_EXIT_ROOM_IN_GAME)
    public void exitRoomInGame(PlayerController playerController, ReqExitRoomInGame reqExitRoomInGame) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
            roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController == null) {
            log.error("玩家请求退出房间，但找不到对应的游戏控制器");
            playerController.send(new RespExitRoomInGame(Code.FAIL));
            return;
        }
        int code = roomManager.exitRoom(playerController);
        playerController.send(new RespExitRoomInGame(code));
        log.debug("玩家请求退出百家乐房间，code: {}", code);
        if (code == Code.SUCCESS) {
            // 需要先将玩家加入临时房间中
            PlayerSessionInfo playerSessionInfo = new PlayerSessionInfo();
            playerSessionInfo.setRoomCfgId(gameController.getRoom().getRoomCfgId());
            baccaratTempRoom.enter(playerController.getSession(), playerController, playerSessionInfo);
        }
    }

    /**
     * 获取所有对应场次的百家乐gameController
     */
    private CommonResult<List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> getBaccaratGameController(int roomCfgId) {
        List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> gameControllers =
            roomManager.getGameControllersByGameType(EGameType.BACCARAT, RoomType.BET_ROOM);
        if (gameControllers.isEmpty()) {
            // 没有找到百家乐的房间
            return new CommonResult<>(Code.FAIL);
        }
        // 房间配置ID
        List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> gameWareControllers =
            gameControllers.stream().filter(controller -> controller.getRoom().getRoomCfgId() == roomCfgId).toList();
        if (gameWareControllers.isEmpty()) {
            return new CommonResult<>(Code.FAIL);
        }
        return new CommonResult<>(Code.SUCCESS, gameWareControllers);
    }

    @Override
    public void doCommand(String command, List<String> params) {
        switch (command) {
            case "getAllGameNode":
                getAllGameNode();
                break;
            default:
                break;
        }
    }

    @Override
    public List<String> needHandleCommands() {
        return List.of("getAllGameNode");
    }
}
