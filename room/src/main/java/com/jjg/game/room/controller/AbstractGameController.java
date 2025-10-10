package com.jjg.game.room.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.BaseFuncProcessor;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.ExceptionUtils;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.TaskConditionParam10003;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.room.base.BaseGameTickTask;
import com.jjg.game.room.base.BaseGameTickTask.ETickTaskType;
import com.jjg.game.room.base.EGameState;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.constant.RoomConstant;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.datatrack.GameDataTracker;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.message.resp.NotifyPauseGameOnNewRound;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.room.timer.RoomTimerCenter;
import com.jjg.game.room.timer.RoomTimerEvent;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.apache.kafka.common.utils.PrimitiveRef;
import org.apache.kafka.common.utils.PrimitiveRef.LongRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
    // 游戏状态枚举
    protected EGameState gameState;
    // tick任务运行时间记录
    private final Map<ETickTaskType, Long> tickTaskTimeRecMap = new HashMap<>();
    // tick任务 tick间隔，执行回调 需要放在tick中检查的必须是周期运行的任务
    protected Map<ETickTaskType, BaseGameTickTask> tickTaskMap = new HashMap<>();
    // 游戏埋点记录
    protected GameDataTracker gameDataTracker;
    // 游戏事件管理器
    protected GameEventManager gameEventManager;
    /**
     * 任务管理器
     */
    protected TaskManager taskManager;

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
        gameState = EGameState.STARTED;
    }

    /**
     * 初始化游戏中的逻辑
     */
    public void initialGame() {
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
        gameState = EGameState.READY;
    }

    @Override
    public <R extends Room> void initial(R room) {
        gameState = EGameState.INIT_DONE;
        // 游戏事件管理器
        this.gameEventManager = roomController.getGameEventManager();
        this.taskManager = roomController.getTaskManager();
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
    public boolean checkRoomCanStart() {
        // 房间玩家不为空
        return !roomController.getRoom().getRoomPlayers().isEmpty() && gameState == EGameState.INIT_DONE;
    }

    /**
     * 玩家加入房间时调用
     *
     * @return 返回进行数据复制后的GamePlayer对象
     */
    public GamePlayer onPlayerJoinRoom(PlayerController playerController, boolean gameStartStatus) {
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
            gameDataVo.setRoomDestroyTime(0);
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
            log.error("玩家游戏数据: {} 更新时间：{} 和数据库中数据的更新时间: {} 不一致, {}",
                    gamePlayer.getId(), gamePlayer.getUpdateTime(), playerUpdateTime, gameDataVo.roomLogInfo());
        } else {
            player = JSON.parseObject(JSON.toJSONString(gamePlayer), Player.class);
            Player finalPlayer = player;
            Player updatedPlayer =
                    roomController.getRoomManager().getPlayerService().doSave(player.getId(), (latestPlayer) -> {
                        // TODO 后续换一种效率更高的复制方法
                        //进行值复制
                        BeanUtils.copyProperties(finalPlayer, latestPlayer);
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
    public void disbandRoom(Boolean disbandRoomByPlayer) {
        // 先暂停房间类的阶段执行逻辑
        gameState = EGameState.DESTROYING;
        if (gameDataTracker != null) {
            // 关闭数据收集
            gameDataTracker.shutdownDataTracker();
        }
    }

    /**
     * 销毁完成
     */
    public void markDestroyed() {
        gameState = EGameState.DESTROYED;
    }

    /**
     * 由房间控制器调用此方法
     */
    @Override
    public void gameDestroy(boolean closeByPlayer, boolean notifyExitRoom) {
        // 暂停游戏
        stopGame();
        // 调用结算逻辑
        gameOverSettlement();
        // 调用房间管理器的解散逻辑
        roomController.getRoomManager().disbandRoom(roomController.getRoom(), closeByPlayer, notifyExitRoom);
    }

    @Override
    public void pauseGame() {
        gameState = EGameState.PAUSING_ON_NEXT_ROUND;
    }

    @Override
    public boolean tryContinueGame() {
        return true;
    }

    /**
     * 广播游戏暂停通知
     */
    public void broadcastGamePauseInfo() {
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

    /**
     * 游戏是否开始 PAUSING_ON_NEXT_ROUND 也需要继续运行，只是在下一轮时不能继续
     */
    public boolean isGameStarted() {
        return gameState == EGameState.STARTED ||
                gameState == EGameState.GAMING ||
                gameState == EGameState.PAUSING_ON_NEXT_ROUND ||
                gameState == EGameState.ROUND_SETTLEMENT ||
                gameState == EGameState.ROUND_OVER;
    }

    @Override
    public void stopGame() {
        // 停止游戏
        gameState = EGameState.STOPING;
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

    public EGameState getGameState() {
        return gameState;
    }

    public GamePlayer getGamePlayer(long playerId) {
        return gameDataVo.getGamePlayer(playerId);
    }

    /**
     * 处理庄家流水，默认是系统，不处理
     *
     * @param bankerFlowing 庄家输赢流水
     */
    public void dealBankerFlowing(long bankerFlowing, Map<Long, SettlementData> settlementDataMap) {
    }

    /**
     * 获取游戏牌桌使用什么道具ID进行交易
     */
    public int getGameTransactionItemId() {
        return gameDataVo.getRoomCfg().getTransactionItemId();
    }

    /**
     * 获取房间内能交易的道具数量
     */
    public long getTransactionItemNum(long playerId) {
        int transactionItemId = getGameTransactionItemId();
        int goldCfgId = ItemUtils.getGoldItemId();
        int diamondCfgId = ItemUtils.getDiamondItemId();
        GamePlayer gamePlayer = getGamePlayer(playerId);
        if (transactionItemId == goldCfgId) {
            return gamePlayer.getGold();
        } else if (transactionItemId == diamondCfgId) {
            return gamePlayer.getDiamond();
        } else {
            log.error("游戏：{} 获取道具 暂不支持其他道具ID：{} 进行交易", getRoom().logStr(), transactionItemId);
            return 0;
        }
    }

    /**
     * 扣除金币
     */
    public int deductItem(long playerId, long num, ERoomItemReason deductType) {
        return deductItem(playerId, num, deductType.name(), deductType.getGameCfgId() + "", true);
    }

    /**
     * 扣除金币
     */
    public int deductItem(long playerId, long num, String deductType) {
        return deductItem(playerId, num, deductType, "", true);
    }

    /**
     * 扣除金币
     */
    public int deductItem(long playerId, long num, String deductType, String desc) {
        return deductItem(playerId, num, deductType, desc, true);
    }

    /**
     * 扣除金币
     *
     * @param playerId   玩家ID
     * @param num        道具数量
     * @param deductType 扣除类型
     * @param desc       描述
     * @param isNotify   是否通知
     * @return 扣除结果
     */
    public int deductItem(long playerId, long num, String deductType, String desc, boolean isNotify) {
        int transactionItemId = getGameTransactionItemId();
        int goldCfgId = ItemUtils.getGoldItemId();
        int diamondCfgId = ItemUtils.getDiamondItemId();
        if (transactionItemId == goldCfgId) {
            return deductGold(playerId, num, deductType, desc, isNotify);
        } else if (transactionItemId == diamondCfgId) {
            return deductDiamond(playerId, num, deductType, desc, isNotify);
        } else {
            log.error("游戏：{} 扣除道具 暂不支持其他道具ID：{} 进行交易", getRoom().logStr(), transactionItemId);
            return Code.FAIL;
        }
    }

    /**
     * 扣除金币，不要将此方法设置为public，游戏的交易道具是配置的道具ID写入
     */
    private int deductGold(long playerId, long num, String deductType, String desc, boolean isNotify) {
        CorePlayerService playerService = roomController.getRoomManager().getPlayerService();
        LongRef beforeUpdateGold = PrimitiveRef.ofLong(0);
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (gamePlayer == null) {
            log.error("异常操作，不能扣除非游戏好友的金币");
            throw new RuntimeException("异常操作，不能扣除非游戏好友的金币");
        }
        if (!(gamePlayer instanceof GameRobotPlayer)) {
            log.info("玩家：{} 扣除金币数量：{}", playerId, num);
        }
        Supplier<GamePlayer> supplier = () -> {
            beforeUpdateGold.value = gamePlayer.getGold();
            long afterCoin = gamePlayer.getGold() - num;
            if (afterCoin < 0) {
                return null;
            }
            gamePlayer.setGold(afterCoin);
            return gamePlayer;
        };
        // 机器人直接扣除
        if (gamePlayer instanceof GameRobotPlayer) {
            supplier.get();
            return Code.SUCCESS;
        }
        // TODO 待修改betDeductGold方法，需要调用betDeductGold扣除金币
        CommonResult<GamePlayer> result =
                playerService.deductGold(playerId, num, deductType, desc, isNotify, supplier, beforeUpdateGold);
        if (result.data == null) {
            return Code.NOT_ENOUGH;
        }
        return result.code;
    }

    /**
     * 扣除金币，不要将此方法设置为public，游戏的交易道具是配置的道具ID写入
     */
    private int deductDiamond(long playerId, long num, String deductType, String desc, boolean isNotify) {
        CorePlayerService playerService = roomController.getRoomManager().getPlayerService();
        LongRef beforeUpdateDiamond = PrimitiveRef.ofLong(0);
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (gamePlayer == null) {
            log.error("异常操作，不能扣除非游戏好友的钻石");
            throw new RuntimeException("异常操作，不能扣除非游戏好友的钻石");
        }
        if (!(gamePlayer instanceof GameRobotPlayer)) {
            log.info("玩家：{} 扣除钻石数量：{}", playerId, num);
        }
        Supplier<GamePlayer> supplier = () -> {
            beforeUpdateDiamond.value = gamePlayer.getDiamond();
            long afterDiamond = gamePlayer.getDiamond() - num;
            if (afterDiamond < 0) {
                return null;
            }
            gamePlayer.setDiamond(afterDiamond);
            return gamePlayer;
        };
        // 机器人直接扣除
        if (gamePlayer instanceof GameRobotPlayer) {
            supplier.get();
            return Code.SUCCESS;
        }
        CommonResult<GamePlayer> result =
                playerService.deductDiamond(playerId, num, deductType, desc, isNotify, supplier, beforeUpdateDiamond);
        if (result.data == null) {
            return Code.NOT_ENOUGH;
        }
        return result.code;
    }

    /**
     * 添加金币
     */
    public int addItem(long playerId, long num, ERoomItemReason addType) {
        return addItem(playerId, num, addType.name(), addType.getGameCfgId() + "", false);
    }

    /**
     * 添加金币
     */
    public int addItem(long playerId, long num, String addType) {
        return addItem(playerId, num, addType, "", false);
    }

    /**
     * 添加金币
     */
    public int addItem(long playerId, long num, String addType, String desc) {
        return addItem(playerId, num, addType, desc, false);
    }

    /**
     * 添加金币
     *
     * @param playerId 玩家ID
     * @param num      金币数量
     * @param addType  添加类型
     * @param desc     描述
     * @param isNotify 是否通知
     * @return 扣除结果
     */
    public int addItem(long playerId, long num, String addType, String desc, boolean isNotify) {
        if (playerId <= 0 || num <= 0) {
            return Code.FAIL;
        }
        int transactionItemId = getGameTransactionItemId();
        int goldCfgId = ItemUtils.getGoldItemId();
        int diamondCfgId = ItemUtils.getDiamondItemId();
        if (transactionItemId == goldCfgId) {
            return addGold(playerId, num, addType, desc, isNotify);
        } else if (transactionItemId == diamondCfgId) {
            return addDiamond(playerId, num, addType, desc, isNotify);
        } else {
            log.error("游戏：{} 添加道具 暂不支持其他道具ID：{} 进行交易", getRoom().logStr(), transactionItemId);
            return Code.FAIL;
        }
    }

    /**
     * 触发任务的方法。用于根据指定的玩家ID、游戏类型和任务参数触发任务。
     *
     * @param playerId 玩家ID
     * @param gameType 游戏类型
     * @param winValue 增加的钱
     * @param coinId   货币id
     */
    private void triggerTask(long playerId, int gameType, long winValue, int coinId) {
        Thread.ofVirtual().start(() -> {
            //触发任务
            taskManager.trigger(playerId, TaskConstant.ConditionType.PLAY_GAME_WIN_MONEY, () -> {
                TaskConditionParam10003 param = new TaskConditionParam10003();
                param.setGameId(gameType);
                param.setAddValue(winValue);
                param.setCoinId(coinId);
                return param;
            });
        });
    }

    /**
     * 添加金币，不要将此方法设置为public，游戏的交易道具是配置的道具ID写入
     *
     * @param player      需要更新数据的player
     * @param currencyMap 货币数量
     * @param addType     添加类型
     * @param desc        描述
     * @param isNotify    是否通知
     */
    public void addCurrency(Player player, Map<Integer, Long> currencyMap, String addType, String desc, boolean isNotify) {
        if (player == null || player.getId() <= 0 || CollectionUtil.isEmpty(currencyMap)) {
            return;
        }
        long playerId = player.getId();
        for (Map.Entry<Integer, Long> entry : currencyMap.entrySet()) {
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
            if (entry.getKey() == ItemUtils.getGoldItemId() && entry.getValue() > 0) {
                if (addGold(playerId, entry.getValue(), addType, desc, isNotify) != Code.SUCCESS) {
                    log.error("房间内添加金币失败 playerId:{} num:{}", playerId, entry.getValue());
                    continue;
                }
                if (gamePlayer != null) {
                    player.setGold(gamePlayer.getGold());
                }
                //触发任务
                triggerTask(playerId, getRoom().getGameType(), entry.getValue(), ItemUtils.getGoldItemId());
            }
            if (entry.getKey() == ItemUtils.getDiamondItemId() && entry.getValue() > 0) {
                if (addDiamond(playerId, entry.getValue(), addType, desc, isNotify) != Code.SUCCESS) {
                    log.error("房间内添加钻石失败 playerId:{} num:{}", playerId, entry.getValue());
                    continue;
                }
                if (gamePlayer != null) {
                    player.setDiamond(gamePlayer.getDiamond());
                }
                //触发任务
                triggerTask(playerId, getRoom().getGameType(), entry.getValue(), ItemUtils.getDiamondItemId());
            }
        }
    }

    /**
     * 添加金币，不要将此方法设置为public，游戏的交易道具是配置的道具ID写入
     *
     * @param playerId 玩家ID
     * @param num      金币数量
     * @param addType  添加类型
     * @param desc     描述
     * @param isNotify 是否通知
     * @return 扣除结果
     */
    private <P extends Player> int addGold(long playerId, long num, String addType, String desc, boolean isNotify) {
        if (playerId <= 0 || num <= 0) {
            return Code.FAIL;
        }
        CorePlayerService playerService = roomController.getRoomManager().getPlayerService();
        LongRef beforeUpdateGold = PrimitiveRef.ofLong(0);
        P gamePlayer = (P) gameDataVo.getGamePlayer(playerId);
        CommonResult<P> result;
        if (gamePlayer != null) {
            if (!(gamePlayer instanceof GameRobotPlayer)) {
                log.info("玩家：{} 添加金币数量：{}", playerId, num);
            }
            Supplier<P> supplier = () -> {
                beforeUpdateGold.value = gamePlayer.getGold();
                gamePlayer.setGold(Math.min(Long.MAX_VALUE, gamePlayer.getGold() + num));
                return gamePlayer;
            };
            // 机器人直接扣除
            if (gamePlayer instanceof GameRobotPlayer) {
                supplier.get();
                return Code.SUCCESS;
            }
            result = playerService.addGold(playerId, num, addType, desc, isNotify, supplier, beforeUpdateGold);
            if (result.data == null) {
                return Code.FAIL;
            }
        } else {
            log.error("异常操作，添加金币时玩家不存在");
            throw new RuntimeException("异常操作，添加金币时玩家不存在");
        }
        return result.code;
    }

    /**
     * 添加钻石，不要将此方法设置为public，游戏的交易道具是配置的道具ID写入
     *
     * @param playerId 玩家ID
     * @param num      钻石数量
     * @param addType  添加类型
     * @param desc     描述
     * @param isNotify 是否通知
     * @return 扣除结果
     */
    private <P extends Player> int addDiamond(long playerId, long num, String addType, String desc, boolean isNotify) {
        CorePlayerService playerService = roomController.getRoomManager().getPlayerService();
        LongRef beforeUpdateGold = PrimitiveRef.ofLong(0);
        P gamePlayer = (P) gameDataVo.getGamePlayer(playerId);
        CommonResult<P> result = new CommonResult<>(Code.FAIL);
        if (gamePlayer != null) {
            if (!(gamePlayer instanceof GameRobotPlayer)) {
                log.info("玩家：{} 添加钻石数量：{}", playerId, num);
            }
            Supplier<P> supplier = () -> {
                beforeUpdateGold.value = gamePlayer.getDiamond();
                gamePlayer.setDiamond(Math.min(Long.MAX_VALUE, gamePlayer.getDiamond() + num));
                return gamePlayer;
            };
            // 机器人直接扣除
            if (gamePlayer instanceof GameRobotPlayer) {
                supplier.get();
                return Code.SUCCESS;
            }
            result = playerService.addDiamond(playerId, num, addType, desc, isNotify, supplier, beforeUpdateGold);
            if (result.data == null) {
                return Code.FAIL;
            }
        } else {
            log.error("异常操作，room: {} 不能添加非游戏好友: {} 的钻石：{} {}",
                    gameDataVo.roomLogInfo(), playerId, num, ExceptionUtils.currentThreadTraces());
        }
        return result.code;
    }

    public GameEventManager getGameEventManager() {
        return gameEventManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }
}
