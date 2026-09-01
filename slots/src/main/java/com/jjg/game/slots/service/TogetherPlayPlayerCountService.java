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
                                notifyPlayers(task);
                            }
                        }.setHandlerParamWithSelf("together play player count")),
                NOTICE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void notifyPlayers(LocalTask task) {
        List<SlotsPlayerGameData> players;
        synchronized (task) {
            if (tasks.get(task.gameType) != task || task.players.isEmpty()) {
                return;
            }
            players = new ArrayList<>(task.players.values());
        }
        try {
            NotifyTogetherPlayPlayerCount notify = new NotifyTogetherPlayPlayerCount();
            notify.playerCount = togetherPlayDao.count(task.gameType) + robotDao.count(task.gameType);
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
        } catch (Exception e) {
            log.error("获取好友同玩人数失败 gameType={}", task.gameType, e);
        } finally {
            synchronized (task) {
                if (tasks.get(task.gameType) == task && !task.players.isEmpty()) {
                    schedule(task);
                }
            }
        }
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
        private Timeout timeout;

        private LocalTask(int gameType) {
            this.gameType = gameType;
        }
    }
}
