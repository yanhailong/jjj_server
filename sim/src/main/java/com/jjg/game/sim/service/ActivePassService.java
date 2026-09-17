package com.jjg.game.sim.service;

import com.jjg.game.sim.dao.ActivePassDao;
import com.jjg.game.sim.data.ActivePassData;
import com.jjg.game.sim.data.ActivePassTask;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.numeric.*;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.bean.PassRewardCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimConditionEventListener;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.pb.res.NotifyActivePassUpdate;
import com.jjg.game.sim.pb.res.ResActivePass;
import com.jjg.game.sim.pb.struct.ActivePassInfo;
import com.jjg.game.sim.pb.struct.ActivePassRewardInfo;
import com.jjg.game.sim.pb.struct.ActivePassTaskInfo;
import com.jjg.game.social.service.SocialSender;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.jjg.game.sim.constant.SimConstant.ActivePass.*;
import static com.jjg.game.sim.constant.SimConstant.MsgBean.*;

/** 活跃通行证聚合，所有写操作由所属SIM节点的玩家线程串行执行。 */
@Service
public class ActivePassService implements SimConditionEventListener, SimPlayerTickListener {
    private final ActivePassConfigService config;
    private final ActivePassDao dao;
    private final SimAutoSaveService saves;
    private final PlayerPackService pack;
    private final MailService mail;
    private final SocialSender sender;
    private final SimPlayerStatService stats;
    private final CorePlayerService players;

    public ActivePassService(ActivePassConfigService config, ActivePassDao dao, SimAutoSaveService saves,
                             PlayerPackService pack, MailService mail, SocialSender sender, SimPlayerStatService stats,
                             CorePlayerService players) {
        this.config = config;
        this.dao = dao;
        this.saves = saves;
        this.pack = pack;
        this.mail = mail;
        this.sender = sender;
        this.stats = stats;
        this.players = players;
    }

    public void initData(SimPlayerContext ctx) {
        long now = System.currentTimeMillis();
        for (ActivePassData old : dao.expired(ctx.playerId(), now)) { settle(old); }
        ensureCurrent(ctx, now);
    }

    /** 换期先结算，日切只替换每日任务，不清积分、周期任务或付费状态。 */
    public ActivePassData ensureCurrent(SimPlayerContext ctx, long now) {
        ActivePassConfigService.Index index = config.index();
        ActivePassConfigService.Period period = index.active(now);
        ActivePassData data = ctx.getActivePassData();
        boolean reset = false;
        if (data != null && (period == null || data.getPassId() != period.id())) {
            settle(data);
            // 历史到账按期读取文档；切换前确保旧期快照已写完。
            saves.awaitPending();
            ctx.setActivePassData(null);
            data = null;
            reset = true;
        }
        if (period != null && data == null) {
            data = dao.findById(ActivePassData.key(ctx.playerId(), period.id())).orElse(null);
            if (data == null) {
                data = new ActivePassData();
                data.setPlayerId(ctx.playerId());
                data.setPassId(period.id());
                data.setEndTime(period.end());
                for (var entry : index.periodic().entrySet()) {
                    data.getPeriodTasks().put(entry.getKey(), newTask(entry.getValue().getFirst().config().getId()));
                }
            }
            ctx.setActivePassData(data);
            reset = true;
        }
        if (data != null) {
            if (data.getEndTime() != period.end()) {
                data.setEndTime(period.end());
                reset = true;
            }
            var date = TimeHelper.getLocalDateTime(now);
            int today = date.getYear() * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
            if (data.getDay() != today) {
                data.setDay(today);
                data.getDailyTasks().clear();
                int allLevel = ctx.getSimBaseData().getAllLevel();
                for (var entry : index.daily().entrySet()) {
                    List<ActivePassConfigService.TaskDefinition> candidates = entry.getValue().stream()
                            .filter(task -> allLevel > task.config().getCasinoLevelMin()).toList();
                    if (!candidates.isEmpty()) {
                        var chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
                        data.getDailyTasks().put(entry.getKey(), newTask(chosen.config().getId()));
                    }
                }
                reset = true;
            }
            if (reset) { saves.enqueueSave(data); }
        }
        if (reset) { notifyUpdate(ctx, true, taskInfos(data)); }
        return data;
    }

    @Override
    public void onTick(SimPlayerContext ctx, long now) { ensureCurrent(ctx, now); }

    @Override
    public ConditionEvent onConditionEvent(SimPlayerContext ctx, ConditionEvent event) {
        if (ctx == null || ctx.getSimBaseData() == null || event == null) { return null; }
        ActivePassData data = ensureCurrent(ctx, System.currentTimeMillis());
        if (data == null) { return null; }
        List<ActivePassTaskInfo> changed = new ArrayList<>();
        advance(data.getDailyTasks().values(), event, changed);
        advance(data.getPeriodTasks().values(), event, changed);
        pollStates(ctx, data, changed);
        if (!changed.isEmpty()) { notifyUpdate(ctx, false, changed); }
        return null;
    }

    private void advance(Collection<ActivePassTask> tasks, ConditionEvent event, List<ActivePassTaskInfo> changed) {
        var index = config.index();
        for (ActivePassTask task : tasks) {
            var definition = index.task(task.getTaskId());
            if (definition == null || task.isClaimed() || task.getProgress() >= definition.condition().target()) { continue; }
            ConditionUpdate update = definition.condition().evaluate(event);
            long next = Math.min(definition.condition().target(), update.apply(task.getProgress()));
            if (next != task.getProgress()) {
                task.setProgress(next);
                changed.add(taskInfo(task));
            }
        }
    }

    /** 状态条件读取当前状态，不把历史累计行为导入当期任务。 */
    private void pollStates(SimPlayerContext ctx, ActivePassData data, List<ActivePassTaskInfo> changed) {
        com.jjg.game.core.data.Player player = null;
        for (ActivePassTask task : allTasks(data)) {
            var def = config.index().task(task.getTaskId());
            if (def == null || task.isClaimed() || task.getProgress() >= def.condition().target()) { continue; }
            PreparedCondition condition = def.condition();
            long previous = task.getProgress();
            if (stats.readsCurrentState(condition)) {
                task.setProgress(Math.min(condition.target(), stats.progress(ctx, condition)));
            } else if (condition.eventType() == StateConditionEvent.class) {
                // 玩家可能正在游戏节点升级，不能使用大厅登录时保留的旧Player快照。
                if (player == null) { player = players.get(ctx.playerId()); }
                if (player == null) { continue; }
                StateConditionEvent event = switch (condition.spec().id()) {
                    case 1 -> new StateConditionEvent(StateConditionEvent.Type.PLAYER_LEVEL, 0, player.getLevel(), 0);
                    case 3 -> new StateConditionEvent(StateConditionEvent.Type.VIP_LEVEL, 0, player.getVipLevel(), 0);
                    default -> null;
                };
                task.setProgress(Math.min(condition.target(), condition.evaluate(event).apply(task.getProgress())));
            }
            if (changed != null && previous != task.getProgress()) { changed.add(taskInfo(task)); }
        }
    }

    public ResActivePass info(SimPlayerContext ctx) {
        ActivePassData data = ensureCurrent(ctx, System.currentTimeMillis());
        if (data != null) { pollStates(ctx, data, null); }
        return response(REQ_ACTIVE_PASS_INFO, Code.SUCCESS, data);
    }

    public ResActivePass claimTask(SimPlayerContext ctx, int passId, int taskId, int day) {
        ActivePassData data = ensureCurrent(ctx, System.currentTimeMillis());
        if (!matches(data, passId)) { return response(REQ_ACTIVE_PASS_TASK_CLAIM, Code.NOT_FOUND, data); }
        var def = config.index().task(taskId);
        if (def == null) { return response(REQ_ACTIVE_PASS_TASK_CLAIM, Code.PARAM_ERROR, data); }
        if (def.config().getTaskType() == TaskConstant.TaskType.ACTIVE_PASS_DAILY && data.getDay() != day) {
            return response(REQ_ACTIVE_PASS_TASK_CLAIM, Code.NOT_FOUND, data);
        }
        Map<Integer, ActivePassTask> tasks = def.config().getTaskType() == TaskConstant.TaskType.ACTIVE_PASS_DAILY
                ? data.getDailyTasks() : data.getPeriodTasks();
        ActivePassTask task = tasks.get(def.config().getGroup());
        if (task == null || task.getTaskId() != taskId) { return response(REQ_ACTIVE_PASS_TASK_CLAIM, Code.NOT_FOUND, data); }
        if (task.isClaimed()) { return response(REQ_ACTIVE_PASS_TASK_CLAIM, Code.SUCCESS, data); }
        pollStates(ctx, data, null);
        if (task.getProgress() < def.condition().target()) { return response(REQ_ACTIVE_PASS_TASK_CLAIM, Code.FAIL, data); }
        // 通行证任务只产生通行证积分，不调用积分大奖的积分入口。
        data.setPoints(Math.addExact(data.getPoints(), def.config().getIntegralNum()));
        task.setClaimed(true);
        if (def.config().getTaskType() == TaskConstant.TaskType.ACTIVE_PASS_PERIOD) {
            var next = config.index().next(def.config().getGroup(), taskId);
            if (next != null) {
                ActivePassTask newTask = newTask(next.config().getId());
                // 相同计数口径的阶段保留已累积值；不同条件从零开始。
                if (def.condition().spec().id() == next.condition().spec().id()
                        && def.condition().progressParameters().equals(next.condition().progressParameters())) {
                    newTask.setProgress(Math.min(task.getProgress(), next.condition().target()));
                }
                tasks.put(def.config().getGroup(), newTask);
            }
        }
        saves.enqueueSave(data);
        notifyUpdate(ctx, false, taskInfos(data));
        return response(REQ_ACTIVE_PASS_TASK_CLAIM, Code.SUCCESS, data);
    }

    public ResActivePass buyPoints(SimPlayerContext ctx, int passId, int count, int expectedPurchasedPoints) {
        ActivePassData data = ensureCurrent(ctx, System.currentTimeMillis());
        if (!matches(data, passId)) { return response(REQ_ACTIVE_PASS_BUY_POINTS, Code.NOT_FOUND, data); }
        var price = config.purchasePrice();
        if (price == null || count <= 0 || expectedPurchasedPoints != data.getPurchasedPoints()) {
            return response(REQ_ACTIVE_PASS_BUY_POINTS, Code.PARAM_ERROR, data);
        }
        long points = (long) count * price.points();
        var period = config.index().period(passId);
        long maximum = period.rewards().getLast().getActivePoints();
        if (points > price.limit() - (long) data.getPurchasedPoints() || data.getPoints() >= maximum
                || points > maximum - data.getPoints() || price.count() > Long.MAX_VALUE / count) {
            return response(REQ_ACTIVE_PASS_BUY_POINTS, Code.PARAM_ERROR, data);
        }
        var player = ctx.getPlayer() == null ? players.get(ctx.playerId()) : ctx.getPlayer();
        if (player == null) { return response(REQ_ACTIVE_PASS_BUY_POINTS, Code.NOT_FOUND, data); }
        var result = pack.removeItems(player, Map.of(price.itemId(), price.count() * count),
                AddType.ACTIVITY, "active-pass-points:" + passId);
        if (result == null || !result.success()) {
            return response(REQ_ACTIVE_PASS_BUY_POINTS, result == null ? Code.EXCEPTION : result.code, data);
        }
        data.setPurchasedPoints(data.getPurchasedPoints() + (int) points);
        data.setPoints(data.getPoints() + points);
        saves.enqueueSave(data);
        notifyUpdate(ctx, false, List.of());
        return response(REQ_ACTIVE_PASS_BUY_POINTS, Code.SUCCESS, data);
    }

    public ResActivePass claimRewards(SimPlayerContext ctx, int passId, int rewardId, int track) {
        ActivePassData data = ensureCurrent(ctx, System.currentTimeMillis());
        if (!matches(data, passId)) { return response(REQ_ACTIVE_PASS_REWARD_CLAIM, Code.NOT_FOUND, data); }
        if (rewardId < 0 || (track != 0 && track != FREE && track != BASIC && track != PREMIUM)) {
            return response(REQ_ACTIVE_PASS_REWARD_CLAIM, Code.PARAM_ERROR, data);
        }
        Map<Integer, Long> items = new LinkedHashMap<>();
        Map<Integer, Integer> claims = new LinkedHashMap<>();
        collect(data, rewardId, track, items, claims);
        ResActivePass response;
        if (!items.isEmpty()) {
            var result = pack.addItems(ctx.playerId(), items, AddType.ACTIVITY, "active-pass:" + passId, true);
            if (result == null || !result.success()) {
                return response(REQ_ACTIVE_PASS_REWARD_CLAIM, result == null ? Code.EXCEPTION : result.code, data);
            }
            data.getClaimedRewards().putAll(claims);
            saves.enqueueSave(data);
        }
        response = response(REQ_ACTIVE_PASS_REWARD_CLAIM, Code.SUCCESS, data);
        response.items = ItemUtils.buildItemInfo(items);
        return response;
    }

    private void collect(ActivePassData data, int rewardId, int requestedTrack,
                         Map<Integer, Long> items, Map<Integer, Integer> claims) {
        var period = config.index().period(data.getPassId());
        if (period == null) { throw new IllegalStateException("历史通行证配置不存在: " + data.getPassId()); }
        for (PassRewardCfg reward : period.rewards()) {
            if (reward.getActivePoints() > data.getPoints() || (rewardId != 0 && reward.getId() != rewardId)) { continue; }
            int mask = data.getClaimedRewards().getOrDefault(reward.getId(), 0);
            for (int track : new int[]{FREE, BASIC, PREMIUM}) {
                if ((requestedTrack != 0 && requestedTrack != track) || (mask & track) != 0
                        || ((FREE | data.getPurchasedTracks()) & track) == 0) { continue; }
                Map<Integer, Long> rewards = rewards(reward, track);
                if (rewards == null || rewards.isEmpty()) { continue; }
                rewards.forEach((id, count) -> items.merge(id, count, Math::addExact));
                mask |= track;
            }
            claims.put(reward.getId(), mask);
        }
    }

    public static Map<Integer, Long> rewards(PassRewardCfg cfg, int track) {
        return switch (track) {
            case FREE -> cfg.getFreeRewards();
            case BASIC -> cfg.getBasicPaidRewards();
            case PREMIUM -> cfg.getPremiumPaidRewards();
            default -> Map.of();
        };
    }

    /** 按期、按轨道幂等补发；邮件失败时保留旧期，下一次访问重试。 */
    public void settle(ActivePassData data) {
        if (data.isSettled()) { return; }
        for (int track : new int[]{FREE, BASIC, PREMIUM}) {
            Map<Integer, Long> items = new LinkedHashMap<>();
            Map<Integer, Integer> claims = new LinkedHashMap<>();
            collect(data, 0, track, items, claims);
            if (!items.isEmpty()) {
                List<Item> attachments = items.entrySet().stream().map(e -> new Item(e.getKey(), e.getValue())).toList();
                if (!mail.addMailIfAbsent(data.getPlayerId(), "活跃通行证奖励", "本期已达成但未领取的奖励已补发，请查收。",
                        attachments, AddType.ACTIVITY, "active-pass-settle:" + data.getPlayerId() + ':' + data.getPassId() + ':' + track)) {
                    throw new IllegalStateException("活跃通行证结算邮件投递失败");
                }
                data.getClaimedRewards().putAll(claims);
            }
        }
        data.setSettled(true);
        data.getDailyTasks().clear();
        data.getPeriodTasks().clear();
        saves.enqueueSave(data);
    }

    public boolean unlock(SimPlayerContext ctx, int passId, int track) {
        if (track != BASIC && track != PREMIUM) { return false; }
        ActivePassData current = ensureCurrent(ctx, System.currentTimeMillis());
        ActivePassData data = matches(current, passId) ? current
                : dao.findById(ActivePassData.key(ctx.playerId(), passId)).orElse(null);
        if (data == null) { return false; }
        if ((data.getPurchasedTracks() & track) == 0) {
            data.setPurchasedTracks(data.getPurchasedTracks() | track);
            data.setSettled(false);
        }
        if (data != current) { settle(data); }
        saveReceipt(data);
        if (data == current) { notifyUpdate(ctx, false, List.of()); }
        return true;
    }

    /** 支付流程只有确认期数据已落库才允许创建订单/确认发货成功。 */
    public void saveReceipt(ActivePassData data) {
        data.buildKey();
        saves.awaitPending();
        dao.save(data);
    }

    private static boolean matches(ActivePassData data, int passId) { return data != null && data.getPassId() == passId; }
    private static ActivePassTask newTask(int id) { ActivePassTask task = new ActivePassTask(); task.setTaskId(id); return task; }
    private List<ActivePassTask> allTasks(ActivePassData data) {
        List<ActivePassTask> tasks = new ArrayList<>(data.getDailyTasks().values());
        tasks.addAll(data.getPeriodTasks().values());
        return tasks;
    }
    private List<ActivePassTaskInfo> taskInfos(ActivePassData data) {
        return data == null ? List.of() : allTasks(data).stream().map(this::taskInfo).filter(Objects::nonNull).toList();
    }
    private ActivePassTaskInfo taskInfo(ActivePassTask task) {
        var def = config.index().task(task.getTaskId());
        if (def == null) { return null; }
        ActivePassTaskInfo info = new ActivePassTaskInfo();
        info.taskId = task.getTaskId();
        info.group = def.config().getGroup();
        info.taskType = def.config().getTaskType();
        info.progress = task.getProgress();
        info.target = def.condition().target();
        info.status = task.isClaimed() ? 2 : info.progress >= info.target ? 1 : 0;
        return info;
    }
    private ResActivePass response(int cmd, int code, ActivePassData data) {
        ResActivePass response = new ResActivePass(code);
        response.requestCmd = cmd;
        response.info = assemble(data);
        return response;
    }
    private ActivePassInfo assemble(ActivePassData data) {
        ActivePassInfo info = new ActivePassInfo();
        if (data == null) { return info; }
        info.passId = data.getPassId();
        info.endTime = data.getEndTime();
        info.points = data.getPoints();
        info.level = config.index().period(info.passId).level(info.points);
        info.purchasedPoints = data.getPurchasedPoints();
        info.purchasedTracks = data.getPurchasedTracks();
        info.day = data.getDay();
        info.tasks = taskInfos(data);
        info.rewards = data.getClaimedRewards().entrySet().stream().map(entry -> {
            ActivePassRewardInfo reward = new ActivePassRewardInfo();
            reward.rewardId = entry.getKey();
            reward.claimedTracks = entry.getValue();
            return reward;
        }).toList();
        return info;
    }
    private void notifyUpdate(SimPlayerContext ctx, boolean reset, List<ActivePassTaskInfo> tasks) {
        ActivePassData data = ctx.getActivePassData();
        NotifyActivePassUpdate message = new NotifyActivePassUpdate(Code.SUCCESS);
        if (data != null) {
            message.passId = data.getPassId();
            message.points = data.getPoints();
            message.level = config.index().period(data.getPassId()).level(data.getPoints());
            message.purchasedTracks = data.getPurchasedTracks();
        }
        message.reset = reset;
        message.tasks = tasks;
        sender.sendTo(ctx.playerId(), message);
    }
}
