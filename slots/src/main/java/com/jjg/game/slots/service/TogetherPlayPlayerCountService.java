package com.jjg.game.slots.service;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.slots.dao.TogetherPlayDao;
import com.jjg.game.slots.dao.TogetherPlayRobotDao;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.pb.NotifyTogetherPlayPlayerCount;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** 每个节点只通知本地玩家，总人数统一读取游戏级 Redis 数据。 */
@Service
public class TogetherPlayPlayerCountService {
    static final long NOTICE_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TogetherPlayDao togetherPlayDao;
    private final TogetherPlayRobotDao robotDao;
    private final Map<Integer, LocalTask> tasks = new ConcurrentHashMap<>();

    public TogetherPlayPlayerCountService(TogetherPlayDao togetherPlayDao,
                                          TogetherPlayRobotDao robotDao) {
        this.togetherPlayDao = togetherPlayDao;
        this.robotDao = robotDao;
    }

    public void onEnter(SlotsPlayerGameData gameData) {
        if (gameData.getEnterType() != 0) {
            return;
        }
        tasks.compute(gameData.getGameType(), (gameType, current) -> {
            LocalTask task = current == null ? new LocalTask(gameType) : current;
            synchronized (task) {
                task.players.put(gameData.getPlayerId(), gameData);
                if (current == null) {
                    task.playerCount = loadPlayerCount(gameType);
                    schedule(task);
                }
            }
            return task;
        });
    }

    public void onExit(SlotsPlayerGameData gameData) {
        tasks.computeIfPresent(gameData.getGameType(), (gameType, task) -> {
            synchronized (task) {
                task.players.remove(gameData.getPlayerId(), gameData);
                if (task.players.isEmpty()) {
                    task.timeout.cancel();
                    return null;
                }
                return task;
            }
        });
    }

    private void schedule(LocalTask task) {
        task.timeout = WheelTimerUtil.schedule(() ->
                PlayerExecutorGroupDisruptor.getDefaultExecutor().publishWithFallback(
                        task.gameType, 0, new BaseHandler<String>() {
                            @Override
                            public void action() {
                                refreshAndNotifyPlayers(task);
                            }
                        }.setHandlerParamWithSelf("together play player count")),
                nextDelay(task), TimeUnit.MILLISECONDS);
    }

    private long nextDelay(LocalTask task) {
        long now = System.currentTimeMillis();
        long baseTime = task.lastNotifyTime == 0 ? now : task.lastNotifyTime;
        task.nextNotifyTime = baseTime + NOTICE_INTERVAL_MILLIS;
        return Math.max(1, task.nextNotifyTime - now);
    }

    private void refreshAndNotifyPlayers(LocalTask task) {
        try {
            synchronized (task) {
                if (tasks.get(task.gameType) != task || task.players.isEmpty()) {
                    return;
                }
            }
            refreshPlayerCount(task);
            notifyPlayers(task);
        } finally {
            synchronized (task) {
                if (tasks.get(task.gameType) == task && !task.players.isEmpty()) {
                    schedule(task);
                }
            }
        }
    }

    private void refreshPlayerCount(LocalTask task) {
        Integer playerCount = loadPlayerCount(task.gameType);
        if (playerCount == null) {
            return;
        }
        synchronized (task) {
            if (tasks.get(task.gameType) == task) {
                task.playerCount = playerCount;
            }
        }
    }

    private Integer loadPlayerCount(int gameType) {
        try {
            return togetherPlayDao.count(gameType) + robotDao.count(gameType);
        } catch (Exception e) {
            log.error("刷新好友同玩人数失败 gameType={}", gameType, e);
            return null;
        }
    }

    private void notifyPlayers(LocalTask task) {
        List<SlotsPlayerGameData> players;
        Integer playerCount;
        synchronized (task) {
            if (tasks.get(task.gameType) != task || task.players.isEmpty()
                    || task.playerCount == null) {
                return;
            }
            players = new ArrayList<>(task.players.values());
            playerCount = task.playerCount;
            task.lastNotifyTime = System.currentTimeMillis();
        }
        NotifyTogetherPlayPlayerCount notify = message(playerCount);
        for (SlotsPlayerGameData gameData : players) {
            if (!gameData.isOnline() || gameData.getPlayerController() == null) {
                continue;
            }
            try {
                gameData.getPlayerController().send(notify);
            } catch (Exception e) {
                log.warn("发送好友同玩人数失败 playerId={},gameType={}",
                        gameData.getPlayerId(), task.gameType, e);
            }
        }
    }

    public void notifyPlayer(SlotsPlayerGameData gameData) {
        if (gameData.getEnterType() != 0 || !gameData.isOnline()
                || gameData.getPlayerController() == null) {
            return;
        }
        Integer playerCount;
        LocalTask task = tasks.get(gameData.getGameType());
        if (task == null) {
            return;
        }
        synchronized (task) {
            if (tasks.get(task.gameType) != task
                    || task.players.get(gameData.getPlayerId()) != gameData
                    || task.playerCount == null) {
                return;
            }
            long timeToNextNotify = task.nextNotifyTime - System.currentTimeMillis();
            if (timeToNextNotify >= 0 && timeToNextNotify < TimeUnit.SECONDS.toMillis(1)) {
                return;
            }
            playerCount = task.playerCount;
        }
        try {
            gameData.getPlayerController().send(message(playerCount));
        } catch (Exception e) {
            log.warn("发送新玩家好友同玩人数失败 playerId={},gameType={}",
                    gameData.getPlayerId(), gameData.getGameType(), e);
        }
    }

    private NotifyTogetherPlayPlayerCount message(int playerCount) {
        NotifyTogetherPlayPlayerCount notify = new NotifyTogetherPlayPlayerCount();
        notify.playerCount = playerCount;
        return notify;
    }

    public void shutdown() {
        tasks.keySet().forEach(gameType -> tasks.computeIfPresent(gameType, (key, task) -> {
            synchronized (task) {
                task.timeout.cancel();
                task.players.clear();
                return null;
            }
        }));
    }

    private static class LocalTask {
        private final int gameType;
        private final Map<Long, SlotsPlayerGameData> players = new HashMap<>();
        private Integer playerCount;
        private long lastNotifyTime;
        private long nextNotifyTime;
        private Timeout timeout;

        private LocalTask(int gameType) {
            this.gameType = gameType;
        }
    }
}
