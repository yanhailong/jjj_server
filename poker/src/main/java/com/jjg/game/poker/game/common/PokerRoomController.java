package com.jjg.game.poker.game.common;

import com.jjg.game.common.concurrent.BaseFuncProcessor;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PokerRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.services.RobotService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.List;


/**
 * poker类的房间控制器
 *
 * @author lm
 */
public class PokerRoomController extends AbstractRoomController<Room_ChessCfg, PokerRoom> {

    public PokerRoomController(Class<? extends RoomPlayer> roomPlayerClazz, PokerRoom room) {
        super(roomPlayerClazz, room);
    }

    @Override
    public void reloadRoomCfg() {
        // 重载配置表引用
        roomCfg = GameDataManager.getRoom_ChessCfg(room.getRoomCfgId());
        gameController.getGameDataVo().reloadRoomCfg();
    }

    @Override
    protected void checkRobotJoinRoom() {
        BaseFuncProcessor baseFuncProcessor = getRoomProcessor();
        // 必须在房间线程中执行
        baseFuncProcessor.executeHandler(new BaseHandler<String>() {
            @Override
            public void action() {
                // 创建人数达到上限
                if (room.getRoomPlayers() != null && room.getRoomPlayers().size() >= room.getMaxLimit()) {
                    return;
                }
                if (robotLastCreatedTime > System.currentTimeMillis()) {
                    return;
                }
                //只有等待开始和准备开始时加入机器人
                if (gameController instanceof BasePokerGameController<? extends BasePokerGameDataVo> controller) {
                    if (!controller.canJoinRobot()) {
                        return;
                    }
                }
                List<Integer> robotIntervalTime = roomCfg.getIntervalTime();
                int randomTime;
                if (robotIntervalTime == null || robotIntervalTime.size() < 2) {
                    randomTime = 1500;
                } else {
                    // 毫秒
                    randomTime = RandomUtils.randomMinMax(robotIntervalTime.getFirst(), robotIntervalTime.getLast());
                }
                // 机器人创建时间更新
                robotLastCreatedTime = System.currentTimeMillis() + randomTime;
                int roomCfgId = roomCfg.getId();
                long robotCreateStartTime = System.currentTimeMillis();
                RobotService robotService = roomManager.getRobotService();
                // 如果房间的
                if (!robotService.checkCanCreateRobot(roomCfgId, room)) {
                    return;
                }
                // 创建一个机器人
                PlayerController robotPlayerController =
                        robotService.getOrCreateRobotPlayerController(roomCfgId, room.getId());
                if (System.currentTimeMillis() - robotCreateStartTime >= 200) {
                    log.debug("机器人创建超时，花费时间：{}", System.currentTimeMillis() - robotCreateStartTime);
                }
                if (robotPlayerController == null) {
                    // 返回
                    return;
                }
                // 将机器人加入房间中
                int code = roomManager.joinRoom(robotPlayerController, room.getGameType(), roomCfgId, room.getId());
                // 如果加入失败则走一次退出房间逻辑
                if (code != Code.SUCCESS) {
                    log.debug("机器人加入房间失败, code : {} {}", code, room.logStr());
                }
            }
        }.setHandlerParamWithSelf("room tick robot join"));
    }

}
