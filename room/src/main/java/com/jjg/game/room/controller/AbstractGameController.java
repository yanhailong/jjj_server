package com.jjg.game.room.controller;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.BaseFuncProcessor;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.room.base.BaseGameTickTask;
import com.jjg.game.room.base.BaseGameTickTask.ETickTaskType;
import com.jjg.game.room.datatrack.GameDataTracker;
import com.jjg.game.room.constant.RoomConstant;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.message.resp.NotifyPauseGameOnNewRound;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.room.timer.RoomTimerCenter;
import com.jjg.game.room.timer.RoomTimerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 抽象游戏流程控制器
 *
 * @author 2CL
 */
public abstract class AbstractGameController<RC extends RoomCfg, G extends GameDataVo<RC>> implements TimerListener<IProcessorHandler>,
    IGameController, IGameLifeCycle {
    protected static final Logger log = LoggerFactory.getLogger(AbstractGameController.class);
    // 游戏配置
    protected G gameDataVo;
    // 游戏控制器
    protected AbstractRoomController<RC, ? extends Room> roomController;
    // 游戏定时器，用于更新游戏中的操作逻辑，不要直接将引用暴露到外面，有需要的逻辑需要在此类中添加
    protected RoomTimerCenter timerCenter;
    // 游戏是否开始
    protected boolean gameStarted = false;
    // 进入下一轮/下一回合时是否暂停
    protected boolean closeGameOnNextRound = false;
    // tick任务运行时间记录
    private final Map<ETickTaskType, Long> tickTaskTimeRecMap = new HashMap<>();
    // tick任务 tick间隔，执行回调 需要放在tick中检查的必须是周期运行的任务
    protected Map<ETickTaskType, BaseGameTickTask> tickTaskMap = new HashMap<>();
    // 游戏埋点记录
    protected GameDataTracker gameDataTracker;

    public AbstractGameController(AbstractRoomController<RC, ? extends Room> roomController) {
        this.roomController = roomController;
    }

    /**
     * 开始游戏
     */
    @Override
    public void startGame() {
        // 记录开始时间
        gameDataVo.setStartTime(System.currentTimeMillis());
        // 标记房间开始运行
        gameStarted = true;
    }

    @Override
    public void initial() {
        // 玩家数据回存检查间隔时间
        int playerSaveCheckInterval =
            RandomUtils.randomMinMax(
                RoomConstant.PLAYER_SAVE_CHECK_INTERVAL_MIN, RoomConstant.PLAYER_SAVE_CHECK_INTERVAL_MAX);
        tickTaskMap.put(ETickTaskType.ROOM_PLAYER_SAVE_CHECK,
            new BaseGameTickTask(playerSaveCheckInterval) {
                @Override
                public void run(long triggeredTimestamp) {
                    checkAndSavePlayerData();
                }
            });
    }

    /**
     * 是否能退出游戏
     *
     * @param playerId 玩家id
     * @return true 能 false 不能
     */
    public boolean canExitGame(long playerId) {
        return true;
    }

    /**
     * 检查房间开局逻辑,默认房间进入玩家并且房间未开始，则开启房间逻辑，实际的房间开启逻辑需要自行判断
     */
    protected boolean checkRoomCanStart() {
        // 房间玩家不为空
        return !roomController.getRoom().getRoomPlayers().isEmpty() && !gameStarted;
    }

    /**
     * 玩家加入房间时调用
     *
     * @return 返回进行数据复制后的GamePlayer对象
     */
    protected GamePlayer onPlayerJoinRoom(PlayerController playerController, boolean gameStartStatus) {
        // 将玩家数据复制到玩家游戏数据中
        CorePlayerService playerService = roomController.getRoomManager().getPlayerService();
        Player player = playerController.isRobotPlayer()
            ? playerController.getPlayer()
            : playerService.getOrUpdatePlayerController(playerController);
        String playerJson = JSON.toJSONString(player);
        GamePlayer gamePlayer;
        if (player instanceof RobotPlayer) {
            gamePlayer = JSON.parseObject(playerJson, GameRobotPlayer.class);
            gameDataVo.addGamePlayer(gamePlayer);
        } else {
            gamePlayer = JSON.parseObject(playerJson, GamePlayer.class);
            gameDataVo.addGamePlayer(gamePlayer);
        }
        return gamePlayer;
    }

    /**
     * 玩家发送房间初始信息 客户端在刚进入房间时，不能收到服务端的主动推送，所以需要等客户端初始化完成后，主动向服务端请求
     */
    public abstract void respRoomInitInfo(PlayerController playerController);

    /**
     * 通过初始的RoomDataVo基类，子类自行实例化符合当前的类的VO对象
     */
    protected abstract G createRoomDataVo(RC roomCfg);

    /**
     * 发送消息 消息
     */
    public <M extends AbstractMessage> void broadcastToPlayers(RoomMessageBuilder<M> message) {
        roomController.broadcastToPlayers(message);
    }


    /**
     * 初始化计时器
     */
    public void initTimerCenter(RoomTimerCenter timerCenter) {
        this.timerCenter = timerCenter;
    }

    // 更新游戏
    @Override
    public void onTimer(TimerEvent<IProcessorHandler> event) {
        if (event == null || event.getParameter() == null) {
            return;
        }
        try {
            // 执行事件的回调
            event.getParameter().action();
        } catch (Exception ex) {
            log.error("房间内的定时器更新逻辑异常, {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public <R extends Room> CommonResult<R> onPlayerLeaveRoom(PlayerController playerController) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayerMap().get(playerController.playerId());
        // 从玩家列表中移除玩家数据，子类的gameDataVo有和玩家相关的临时数据需要自行删除
        gameDataVo.getGamePlayerMap().remove(playerController.playerId());
        // 玩家退出时直接回存玩家数据，需要放在游戏离开逻辑最后
        directlySavePlayerData(gamePlayer);
        return new CommonResult<>(Code.SUCCESS);
    }

    /**
     * 回存玩家基础数据、在timeTick中调用
     */
    private void checkAndSavePlayerData() {
        // TODO 还需要优化回存检查
        // 真人玩家进行数据回存
        gameDataVo.getGamePlayerMapExceptRobot().values().forEach(gamePlayer -> {
            int randomTime =
                RandomUtils.randomMinMax(RoomConstant.PLAYER_SAVE_DB_TIME_MIN, RoomConstant.PLAYER_SAVE_DB_TIME_MAX);
            // 每个玩家添加随机回存time事件
            addGameTimeEvent(
                new TimerEvent<>(this, randomTime, () -> this.directlySavePlayerData(gamePlayer)),
                RoomEventType.ROOM_SAVE_PLAYER_DATA);
        });
    }

    /**
     * 直接回存玩家数据
     */
    protected void directlySavePlayerData(GamePlayer gamePlayer) {
        if (gamePlayer instanceof GameRobotPlayer) {
            return;
        }
        Player player = roomController.getRoomManager().getPlayerService().get(gamePlayer.getId());
        long playerUpdateTime = player.getUpdateTime();
        // 如果游戏端的更新时间和数据库中的不一致，说明玩家数据在游戏外部进行了修改，需要判断使用哪一边的数据，暂时先打日志
        if (gamePlayer.getUpdateTime() != playerUpdateTime) {
            log.error("玩家游戏数据: {} 更新时间：{} 和数据库中数据的更新时间: {} 不一致",
                gamePlayer.getId(), gamePlayer.getUpdateTime(), playerUpdateTime);
        } else {
            player = JSON.parseObject(JSON.toJSONString(gamePlayer), Player.class);
            Player finalPlayer = player;
            Player updatedPlayer =
                roomController.getRoomManager().getPlayerService().doSave(player.getId(), (latestPlayer) -> {
                    //进行值复制
                    BeanUtils.copyProperties(latestPlayer, finalPlayer);
                });
            gamePlayer.setUpdateTime(updatedPlayer.getUpdateTime());
            log.info("回存玩家: {} 游戏数据成功", finalPlayer.getId());
        }
    }

    @Override
    public void reconnect() {

    }

    @Override
    public void hosting() {
        // 玩家挂机之后，将玩家状态切位挂机状态
    }

    @Override
    public void roomTick() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<ETickTaskType, BaseGameTickTask> entry : tickTaskMap.entrySet()) {
            if (!tickTaskTimeRecMap.containsKey(entry.getKey())) {
                tickTaskTimeRecMap.put(entry.getKey(), currentTime);
            }
            long latestRunTime = tickTaskTimeRecMap.get(entry.getKey());
            if (latestRunTime >= currentTime) {
                continue;
            }
            // 更新tick任务下次触发时间
            tickTaskTimeRecMap.put(entry.getKey(), currentTime + entry.getValue().getTaskInterval());
            BaseFuncProcessor baseFuncProcessor = roomController.getRoomProcessor();
            // 运行tick任务, 需要在房间线程中排队执行，不能阻塞正常的tick，不然会导致 Do Overtime
            baseFuncProcessor.executeHandler(new BaseHandler<String>() {
                @Override
                public void action() {
                    entry.getValue().run(currentTime);
                }
            }.setHandlerParamWithSelf("room tick"));
        }
    }

    @Override
    public void roomReady() {

    }

    @Override
    public void disbandRoom() {
        // 先暂停房间类的阶段执行逻辑
        gameStarted = false;
        // 关闭数据收集
        gameDataTracker.shutdownDataTracker();
    }

    /**
     * 由房间控制器调用此方法
     */
    @Override
    public void gameOver() {
        // 暂停游戏
        stopGame();
        // 调用结算逻辑
        gameOverSettlement();
        // 调用房间管理器的解散逻辑
        roomController.getRoomManager().disbandRoom(roomController.getRoom());
    }

    @Override
    public void pauseGame() {
        closeGameOnNextRound = true;
    }

    /**
     * 广播游戏暂停通知
     */
    public void broadcastGamePauseInfo(){
        NotifyPauseGameOnNewRound notifyPauseGameOnNewRound = new NotifyPauseGameOnNewRound();
        broadcastToPlayers(
            RoomMessageBuilder.newBuilder().setData(notifyPauseGameOnNewRound).toAllPlayer());
    }

    @Override
    public void gameOverSettlement() {
        // 整局结束进入大结算
    }

    public G getGameDataVo() {
        return gameDataVo;
    }

    public void setGameDataVo(G gameDataVo) {
        this.gameDataVo = gameDataVo;
    }


    /**
     * 给游戏添加定时器
     */
    public void addGameTimeEvent(TimerEvent<IProcessorHandler> roomUpdateTimer, RoomEventType roomEventType) {
        timerCenter.add(new RoomTimerEvent<>(roomUpdateTimer, roomController.getRoom(), roomEventType));
    }


    /**
     * 获取房间统计信息
     */
    public void getRoomStatistics() {

    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    @Override
    public void stopGame() {
        gameStarted = false;
        gameDataVo.setStopTime(System.currentTimeMillis());
        // 房间结束前调用
        beforeDestroyRoom();
        // 暂停计时器运行
        this.timerCenter.remove(this);
    }

    public AbstractRoomController<RC, ? extends Room> getRoomController() {
        return roomController;
    }

    public <R extends Room> R getRoom() {
        return (R) roomController.getRoom();
    }

    public GameDataTracker getGameDataTracker() {
        return gameDataTracker;
    }

    public boolean isCloseGameOnNextRound() {
        return closeGameOnNextRound;
    }
}
