package com.jjg.game.slots.service;

import cn.hutool.core.lang.WeightRandom;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.TogetherPlayRobotDao;
import com.jjg.game.slots.dao.TogetherPlayRobotDao.DisplayData;
import com.jjg.game.slots.dao.TogetherPlayRobotDao.DisplayRobot;
import com.jjg.game.slots.dao.TogetherPlayRobotDao.Schedule;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.pb.NotifyPlayerRewards;
import com.jjg.game.slots.pb.TogetherPlayPlayerInfo;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 仅生成共享展示数据，不进入真人集合，不参与下注、资产和提成结算。 */
@Service
public class TogetherPlayRobotService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final RobotUtil robotUtil;
    private final TogetherPlayRobotDao robotDao;
    private final TogetherPlayRewardNotifier rewardNotifier;
    // 节点内只保存任务和本地观看者；机器人名单、加入时间和执行进度全部以 Redis 为准。
    private final Map<Integer, LocalSchedule> schedules = new ConcurrentHashMap<>();

    public TogetherPlayRobotService(RobotUtil robotUtil, TogetherPlayRobotDao robotDao,
                                    TogetherPlayRewardNotifier rewardNotifier) {
        this.robotUtil = robotUtil;
        this.robotDao = robotDao;
        this.rewardNotifier = rewardNotifier;
    }

    public void onEnter(SlotsPlayerGameData gameData) {
        if (gameData.getEnterType() != 0) {
            return;
        }
        BaseRoomCfg cfg = GameDataManager.getBaseRoomCfg(gameData.getRoomCfgId());
        if (cfg == null || cfg.getRobot_num() == null || cfg.getRobot_num().isEmpty()) {
            return;
        }
        schedules.compute(gameData.getGameType(), (gameType, current) -> {
            LocalSchedule local = current;
            if (local == null) {
                if (!validRange(cfg.getIntervalTime()) || !validRange(cfg.getInterval())
                        || !validWeights(cfg.getRobotBet()) || !validWeights(cfg.getWinMultiplier())
                        || cfg.getRobot_num().stream().anyMatch(row -> row == null || row.size() != 3)) {
                    log.warn("好友同玩机器人配置无效，停止展示 gameType={}", gameType);
                    return null;
                }
                if (cfg.getRobot_num().stream().noneMatch(row -> row.get(2) > 0)) {
                    return null;
                }
                local = new LocalSchedule(gameType, cfg);
            }
            synchronized (local) {
                local.viewers.put(gameData.getPlayerId(), gameData);
                if (current == null) {
                    schedule(local, 0);
                }
            }
            return local;
        });
    }

    public void onExit(SlotsPlayerGameData gameData) {
        schedules.computeIfPresent(gameData.getGameType(), (gameType, local) -> {
            synchronized (local) {
                local.viewers.remove(gameData.getPlayerId(), gameData);
                if (local.viewers.isEmpty()) {
                    local.timeout.cancel();
                    return null;
                }
                return local;
            }
        });
    }

    public Map<Long, TogetherPlayPlayerInfo> playerInfos(int gameType, int roomCfgId) {
        DisplayData data = robotDao.get(gameType);
        if (data == null) {
            return Map.of();
        }
        BaseRoomCfg cfg = GameDataManager.getBaseRoomCfg(roomCfgId);
        long now = System.currentTimeMillis();
        Map<Long, TogetherPlayPlayerInfo> infos = new HashMap<>(data.robots().size());
        for (DisplayRobot robot : data.robots()) {
            TogetherPlayPlayerInfo info = new TogetherPlayPlayerInfo();
            info.playerId = robot.playerId();
            info.nick = robot.nick();
            info.headImg = robot.headImg();
            info.headFrame = robot.headFrame();
            // 使用共享加入时间按需计算展示增长，不进行周期性金币写入。
            info.winGold = Math.max(0, now - robot.joinTime()) * cfg.getGrowthRate()
                    / TimeUnit.MINUTES.toMillis(1);
            infos.put(info.playerId, info);
        }
        return infos;
    }

    private void schedule(LocalSchedule local, long delayMillis) {
        local.timeout = WheelTimerUtil.schedule(() ->
                PlayerExecutorGroupDisruptor.getDefaultExecutor().publishWithFallback(
                        local.gameType, 0, new BaseHandler<String>() {
                            @Override
                            public void action() {
                                tick(local);
                            }
                        }.setHandlerParamWithSelf("together play robot")), delayMillis, TimeUnit.MILLISECONDS);
    }

    private void tick(LocalSchedule local) {
        NotifyPlayerRewards notify = null;
        synchronized (local) {
            if (schedules.get(local.gameType) != local || local.viewers.isEmpty()) {
                return;
            }
            long delay = SlotsConst.Common.TOGETHER_PLAY_ROBOT_RETRY_MILLIS;
            try {
                DisplayData data = robotDao.get(local.gameType);
                Schedule before = data == null ? null : data.schedule();
                long now = System.currentTimeMillis();
                boolean join = before != null && now >= before.nextJoinTime();
                boolean reward = before != null && now >= before.nextRewardTime();
                List<DisplayRobot> robots = data == null ? List.of() : data.robots();
                boolean leave = before != null && now >= nextLeaveTime(robots, before.lastLeaveCheckTime());
                Schedule after = before;
                if (before == null || join || reward || leave) {
                    robots = new ArrayList<>(robots);
                    boolean changed = leave && leaveRobots(robots, before.lastLeaveCheckTime(), now);
                    if (join) {
                        changed |= joinRobot(local, robots, now);
                    }
                    after = new Schedule(before == null || join
                            ? nextTime(now, local.cfg.getIntervalTime(), TimeUnit.MILLISECONDS) : before.nextJoinTime(),
                            before == null || reward
                            ? nextTime(now, local.cfg.getInterval(), TimeUnit.SECONDS) : before.nextRewardTime(),
                            before == null || leave ? now : before.lastLeaveCheckTime());
                    if (robotDao.save(local.gameType, before, after, before == null || changed ? robots : null)) {
                        if (reward) {
                            notify = rewards(local, robots);
                        }
                    } else {
                        // 其他节点已推进，或全局无人而暂停推进；下次读取最新进度。
                        after = null;
                    }
                }
                if (after != null) {
                    long next = Math.min(Math.min(after.nextJoinTime(), after.nextRewardTime()),
                            nextLeaveTime(robots, after.lastLeaveCheckTime()));
                    delay = Math.max(1, next - System.currentTimeMillis());
                }
            } catch (Exception e) {
                log.error("好友同玩机器人展示异常 gameType={}", local.gameType, e);
            } finally {
                schedule(local, delay);
            }
        }
        if (notify != null) {
            // 复用真人的分批会话路由，覆盖其他 Slots 节点的玩家，提成恒为 0。
            rewardNotifier.notify(local.gameType, notify, Map.of());
        }
    }

    private long nextTime(long now, List<Integer> range, TimeUnit unit) {
        // 即使配置为零间隔，也必须推进时间戳，防止同一进度被多个节点重复领取。
        return now + Math.max(1, unit.toMillis(RandomUtils.randomLongMinMax(range.getFirst(), range.getLast())));
    }

    private long nextLeaveTime(List<DisplayRobot> robots, long checkedTime) {
        long next = Long.MAX_VALUE;
        for (DisplayRobot robot : robots) {
            next = Math.min(next, nextLeaveTime(robot, checkedTime));
        }
        return next;
    }

    private long nextLeaveTime(DisplayRobot robot, long checkedTime) {
        List<Integer> leave = GameDataManager.getRobotCfg(robot.cfgId()).getLeaveRoom();
        if (leave.getFirst() <= 0 || leave.getLast() <= 0) {
            return Long.MAX_VALUE;
        }
        long interval = TimeUnit.MINUTES.toMillis(leave.getLast());
        // 检查进度全局共享，周期仍以每个机器人的加入时间为起点，不被其他机器人的检查打乱。
        long elapsed = Math.max(0, checkedTime - robot.joinTime());
        return robot.joinTime() + (elapsed / interval + 1) * interval;
    }

    private boolean leaveRobots(List<DisplayRobot> robots, long checkedTime, long now) {
        return robots.removeIf(robot -> now >= nextLeaveTime(robot, checkedTime)
                && RandomUtils.randomInt(GameConstant.TEN_THOUSAND)
                < GameDataManager.getRobotCfg(robot.cfgId()).getLeaveRoom().getFirst());
    }

    private boolean joinRobot(LocalSchedule local, List<DisplayRobot> robots, long now) {
        Map.Entry<Integer, Integer> entry = local.limits.ceilingEntry(LocalTime.now().getHour());
        int limit = entry == null ? 0 : Math.max(0, entry.getValue());
        boolean changed = robots.size() > limit;
        if (changed) {
            robots.subList(limit, robots.size()).clear();
        }
        List<RobotCfg> configs = GameDataManager.getRobotCfgList();
        if (robots.size() >= limit || robots.size() >= configs.size()) {
            return changed;
        }
        // 配置 ID 与节点无关，接替调度的节点不能按自己的机器人 ID 判断是否重复。
        Set<Integer> usedConfigs = robots.stream().map(DisplayRobot::cfgId).collect(Collectors.toSet());
        int start = RandomUtils.randomInt(configs.size());
        for (int i = 0; i < configs.size(); i++) {
            RobotCfg cfg = configs.get((start + i) % configs.size());
            if (usedConfigs.contains(cfg.getId())) {
                continue;
            }
            RobotPlayer player = robotUtil.initRobotPlayer(cfg);
            robots.add(new DisplayRobot(cfg.getId(), player.getId(), player.getNickName(),
                    player.getHeadImgId(), player.getHeadFrameId(), now));
            return true;
        }
        return changed;
    }

    private NotifyPlayerRewards rewards(LocalSchedule local, List<DisplayRobot> robots) {
        if (robots.isEmpty()) {
            return null;
        }
        DisplayRobot robot = robots.get(RandomUtils.randomInt(robots.size()));
        int bet = local.bets.next();
        int times = local.multipliers.next();
        if (times <= SlotsConst.Common.TOGETHER_PLAY_NOTIFY_MIN_TIMES || bet <= 0) {
            return null;
        }
        NotifyPlayerRewards notify = new NotifyPlayerRewards();
        notify.playerId = robot.playerId();
        notify.headImg = robot.headImg();
        notify.headFrame = robot.headFrame();
        notify.rewards = (long) bet * times;
        notify.times = times;
        return notify;
    }

    private boolean validRange(List<Integer> range) {
        return range != null && range.size() == 2 && range.getFirst() >= 0 && range.getLast() >= range.getFirst();
    }

    private boolean validWeights(Map<Integer, Integer> weights) {
        return weights != null && weights.keySet().stream().anyMatch(weight -> weight > 0);
    }

    public void shutdown() {
        // 仅取消本节点任务，不能清除仍被其他节点玩家使用的共享数据。
        schedules.keySet().forEach(gameType -> schedules.computeIfPresent(gameType, (key, local) -> {
            synchronized (local) {
                local.timeout.cancel();
                local.viewers.clear();
                return null;
            }
        }));
    }

    private static class LocalSchedule {
        private final int gameType;
        private final BaseRoomCfg cfg;
        private final TreeMap<Integer, Integer> limits = new TreeMap<>();
        private final WeightRandom<Integer> bets = new WeightRandom<>();
        private final WeightRandom<Integer> multipliers = new WeightRandom<>();
        private final Map<Long, SlotsPlayerGameData> viewers = new HashMap<>();
        private Timeout timeout;

        private LocalSchedule(int gameType, BaseRoomCfg cfg) {
            this.gameType = gameType;
            this.cfg = cfg;
            cfg.getRobot_num().forEach(row -> limits.put(row.get(1), row.get(2)));
            cfg.getRobotBet().forEach((weight, value) -> bets.add(value, weight));
            cfg.getWinMultiplier().forEach((weight, value) -> multipliers.add(value, weight));
        }
    }
}
