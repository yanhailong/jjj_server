package com.jjg.game.sim.service;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.sim.constant.CoopTaskConst;
import com.jjg.game.sim.dao.CoopRoomRecordDao;
import com.jjg.game.sim.dao.SimCoopTaskDao;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.pb.res.*;
import com.jjg.game.sim.pb.struct.CoopTaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 多人协作任务服务 (任务层: 每日池/刷新/领取/领奖/结算回写)。
 * <p>
 * 与主线/成就任务 ({@link SimTaskService}) 完全独立: 生命周期为"每日池领取 -> slots 协作房间
 * -> 团队结算 -> 领奖", 进度由 slots 房间内存聚合, 不走 core 条件计数系统, 任务层零轮询零计数 IO。
 * 每日重置按 dayKey 懒触发, 无全服定时任务。
 *
 * @author 11
 * @date 2026/7/6
 */
@Service
public class SimCoopTaskService {
    private static final Logger log = LoggerFactory.getLogger(SimCoopTaskService.class);
    private static final long SETTLEMENT_RECEIPT_RETENTION_MS = 2L * 24 * 3600 * 1000;

    @Autowired
    private CoopTaskConfigService configService;
    @Autowired
    private SimCoopTaskDao coopTaskDao;
    @Autowired
    private CoopRoomRecordDao roomRecordDao;
    @Autowired
    private MarsCurator marsCurator;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private MailService mailService;
    @Autowired
    private SimSkillService simSkillService;
    @Autowired
    private SimSkillsDao simSkillsDao;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;

    // =====================================================================
    // 加载 / 每日重置
    // =====================================================================

    /**
     * 登录加载 (挂入 ctx, 复用 sim 装配链)。
     */
    public void initData(SimPlayerContext ctx) {
        SimCoopTaskData data = coopTaskDao.findById(ctx.playerId()).orElse(null);
        if (data == null) {
            data = new SimCoopTaskData();
            data.setPlayerId(ctx.playerId());
        }
        ctx.setSimCoopTaskData(data);
        ensureDaily(data);
    }

    /**
     * 每日懒重置: 重抽今日列表/清领取次数/清免费刷新。
     * 所有已领取任务 (CLAIMED/IN_ROOM/REWARDABLE/FAILED) 一律不跨天保留, 全部恢复到"未接取"。
     */
    private boolean ensureDaily(SimCoopTaskData data) {
        int today = todayKey();
        if (data.getDayKey() == today) {
            return true;
        }
        //跨日前先读取并消费已完成记录；无法确认结算时保留旧日数据，下次访问继续处理。
        List<SimCoopTaskEntry> inRoomEntries = data.getTasks().values().stream()
                .filter(entry -> entry.getStatus() == CoopTaskConst.TaskStatus.IN_ROOM && entry.getRoomId() > 0)
                .toList();
        Map<Long, CoopRoomRecord> roomRecords = new HashMap<>();
        for (SimCoopTaskEntry entry : inRoomEntries) {
            try {
                roomRecords.put(entry.getRoomId(), roomRecordDao.get(entry.getRoomId()));
            } catch (Exception e) {
                log.error("多人任务跨日读取房间失败,暂缓重置 playerId={},roomId={}",
                        data.getPlayerId(), entry.getRoomId(), e);
                return false;
            }
        }
        List<Long> settledRoomIds = new ArrayList<>();
        for (SimCoopTaskEntry entry : inRoomEntries) {
            CoopRoomRecord record = roomRecords.get(entry.getRoomId());
            if (record == null || record.getStatus() != CoopTaskConst.RoomStatus.FINISHED) {
                continue;
            }
            int settledStatus = record.isSuccess()
                    ? CoopTaskConst.TaskStatus.REWARDABLE : CoopTaskConst.TaskStatus.FAILED;
            long finishTime = System.currentTimeMillis();
            boolean settled;
            boolean receiptExists;
            try {
                settled = onSettle(null, data.getPlayerId(), entry.getTaskId(), entry.getRoomId(),
                        record.isSuccess(), record.getSettlementHelperIds());
                receiptExists = settled || coopTaskDao.isEntrySettled(
                        data.getPlayerId(), entry.getTaskId(), entry.getRoomId(), settledStatus);
            } catch (Exception e) {
                log.error("多人任务跨日结算异常,暂缓重置 playerId={},taskId={},roomId={}",
                        data.getPlayerId(), entry.getTaskId(), entry.getRoomId(), e);
                return false;
            }
            if (!receiptExists) {
                log.error("多人任务跨日结算未确认,暂缓重置 playerId={},taskId={},roomId={}",
                        data.getPlayerId(), entry.getTaskId(), entry.getRoomId());
                return false;
            }
            data.getSettlementReceipts().putIfAbsent(entry.getRoomId(),
                    new CoopSettlementReceipt(entry.getTaskId(), settledStatus, finishTime));
            if (settled) {
                settledRoomIds.add(entry.getRoomId());
            }
        }
        for (SimCoopTaskEntry entry : inRoomEntries) {
            CoopRoomRecord record = roomRecords.get(entry.getRoomId());
            if (record != null && record.getStatus() == CoopTaskConst.RoomStatus.FINISHED
                    && !settledRoomIds.contains(entry.getRoomId())) {
                continue;
            }
            try {
                releaseRoomRecord(data.getPlayerId(), entry.getRoomId(), record);
            } catch (Exception e) {
                //记录删除失败不影响重置；房间记录自身 TTL 或结算重试仍会继续收敛。
                log.error("多人任务每日清理房间记录失败 playerId={},roomId={}",
                        data.getPlayerId(), entry.getRoomId(), e);
            }
        }
        data.setDayKey(today);
        data.setFreeRefreshUsed(false);
        data.setClaimedCount(0);
        //全清: 任一状态都不跨天保留。
        data.getTasks().clear();
        long receiptExpireBefore = System.currentTimeMillis() - SETTLEMENT_RECEIPT_RETENTION_MS;
        data.getSettlementReceipts().values().removeIf(receipt -> receipt.getFinishTime() < receiptExpireBefore);
        data.setPoolTaskIds(drawTasks(configService.getDailyPoolCount(), data.getTasks().keySet()));
        log.info("多人任务每日重置(全清) playerId={},pool={}", data.getPlayerId(), data.getPoolTaskIds());
        return true;
    }

    /**
     * 从任务池随机抽取 count 个互不重复且不与 exclude 重复的任务 (部分 Fisher-Yates, O(count))。
     */
    private List<Integer> drawTasks(int count, Set<Integer> exclude) {
        List<Integer> pool = new ArrayList<>(configService.getPoolTaskIds());
        if (exclude != null && !exclude.isEmpty()) {
            pool.removeAll(exclude);
        }
        int n = Math.min(count, pool.size());
        //count<=0 兜底 (如 dailyPoolCount 热更调小后 kept 超额), 防 subList 负下标
        if (n <= 0) {
            return new ArrayList<>();
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            int j = i + random.nextInt(pool.size() - i);
            Integer tmp = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, tmp);
        }
        return new ArrayList<>(pool.subList(0, n));
    }

    // =====================================================================
    // 列表 / 刷新
    // =====================================================================

    public ResCoopTaskList buildTaskList(SimPlayerContext ctx) {
        ResCoopTaskList res = new ResCoopTaskList(Code.SUCCESS);
        SimCoopTaskData data = ctx.getSimCoopTaskData();
        if (data == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        if (!ensureDaily(data)) {
            res.code = Code.FAIL;
            return res;
        }
        selfHealRooms(ctx, data);

        //今日池 + 跨天保留的已领取任务 (顺序: 池序在前)
        LinkedHashSet<Integer> ids = new LinkedHashSet<>(data.getPoolTaskIds());
        ids.addAll(data.getTasks().keySet());
        List<CoopTaskInfo> tasks = new ArrayList<>(ids.size());
        for (Integer taskId : ids) {
            tasks.add(toInfo(taskId, data.getTasks().get(taskId)));
        }
        res.tasks = tasks;
        res.remainClaimCount = Math.max(0, configService.getDailyClaimLimit() - data.getClaimedCount());
        res.freeRefresh = !data.isFreeRefreshUsed();
        res.refreshItemId = configService.getRefreshCostItemId();
        res.refreshItemCount = configService.getRefreshCostCount();
        res.nextRefreshTime = nextMidnightMillis();
        res.dailyClaimLimit = configService.getDailyClaimLimit();
        return res;
    }

    /**
     * 刷新今日列表: 每日首次免费, 之后每次消耗道具; 已领取任务保留原位不刷走。
     */
    public ResCoopTaskRefresh refresh(SimPlayerContext ctx) {
        ResCoopTaskRefresh res = new ResCoopTaskRefresh(Code.FAIL);
        SimCoopTaskData data = ctx.getSimCoopTaskData();
        Player player = resolvePlayer(ctx);
        if (data == null || player == null) {
            res.code = Code.NOT_FOUND;
            log.warn("多人任务刷新失败,数据缺失 playerId={}", ctx.playerId());
            return res;
        }
        if (!ensureDaily(data)) {
            return res;
        }

        if (data.isFreeRefreshUsed()) {
            int itemId = configService.getRefreshCostItemId();
            long count = configService.getRefreshCostCount();
            if (itemId <= 0 || count <= 0) {
                log.warn("多人任务刷新失败,刷新道具配置缺失 playerId={},itemId={},count={}", ctx.playerId(), itemId, count);
                return res;
            }
            //先扣后刷; 扣除失败直接返回, 不动列表
            if (!playerPackService.removeItems(player, Map.of(itemId, count),
                    AddType.SIM_COOP_TASK_REFRESH).success()) {
                log.info("多人任务刷新道具不足 playerId={},itemId={},count={}", ctx.playerId(), itemId, count);
                res.code = Code.DIAMOND_NOT_ENOUGH;
                return res;
            }
        }

        //已领取任务保留原位, 其余槽位重抽 (排除保留项避免重复)
        List<Integer> kept = new ArrayList<>();
        for (Integer taskId : data.getPoolTaskIds()) {
            if (data.getTasks().containsKey(taskId)) {
                kept.add(taskId);
            }
        }
        List<Integer> fresh = drawTasks(configService.getDailyPoolCount() - kept.size(),
                data.getTasks().keySet());
        List<Integer> pool = new ArrayList<>(kept);
        pool.addAll(fresh);
        data.setPoolTaskIds(pool);
        data.setFreeRefreshUsed(true);
        ctx.setLastSaveTime(0);

        res.code = Code.SUCCESS;
        res.freeRefresh = false;
        List<CoopTaskInfo> tasks = new ArrayList<>(pool.size());
        for (Integer taskId : pool) {
            tasks.add(toInfo(taskId, data.getTasks().get(taskId)));
        }
        res.tasks = tasks;
        log.info("多人任务刷新列表 playerId={},kept={},pool={}", ctx.playerId(), kept, pool);
        return res;
    }

    // =====================================================================
    // 领取 / 领奖
    // =====================================================================

    public ResCoopTaskClaim claim(SimPlayerContext ctx, int taskId) {
        ResCoopTaskClaim res = new ResCoopTaskClaim(Code.FAIL);
        res.taskId = taskId;
        SimCoopTaskData data = ctx.getSimCoopTaskData();
        if (data == null) {
            res.code = Code.NOT_FOUND;
            log.warn("多人任务领取失败,数据缺失 playerId={},taskId={}", ctx.playerId(), taskId);
            return res;
        }
        if (!ensureDaily(data)) {
            return res;
        }
        res.remainClaimCount = Math.max(0, configService.getDailyClaimLimit() - data.getClaimedCount());

        if (!data.getPoolTaskIds().contains(taskId)) {
            log.info("多人任务领取失败,任务不在今日列表 playerId={},taskId={}", ctx.playerId(), taskId);
            return res;
        }
        if (data.getTasks().containsKey(taskId)) {
            log.info("多人任务领取失败,任务已领取 playerId={},taskId={}", ctx.playerId(), taskId);
            return res;
        }
        if (data.getClaimedCount() >= configService.getDailyClaimLimit()) {
            log.info("多人任务领取失败,今日领取次数已用完 playerId={},taskId={},claimedCount={}",
                    ctx.playerId(), taskId, data.getClaimedCount());
            res.code = Code.TODAY_CLIAM_LIMIT;
            return res;
        }
        if (configService.ruleOf(taskId) == null) {
            log.warn("多人任务领取失败,配置非法 playerId={},taskId={}", ctx.playerId(), taskId);
            return res;
        }

        SimCoopTaskEntry entry = new SimCoopTaskEntry();
        entry.setTaskId(taskId);
        entry.setStatus(CoopTaskConst.TaskStatus.CLAIMED);
        entry.setClaimTime(System.currentTimeMillis());
        data.getTasks().put(taskId, entry);
        data.setClaimedCount(data.getClaimedCount() + 1);
        ctx.setLastSaveTime(0);

        res.code = Code.SUCCESS;
        res.remainClaimCount = Math.max(0, configService.getDailyClaimLimit() - data.getClaimedCount());
        log.info("多人任务领取 playerId={},taskId={},claimedCount={}", ctx.playerId(), taskId, data.getClaimedCount());
        return res;
    }

    /**
     * 发起者领取任务奖励: 校验待领奖态 -> 发奖 -> 任务从列表移除。
     */
    public ResCoopTaskReward claimReward(SimPlayerContext ctx, int taskId) {
        ResCoopTaskReward res = new ResCoopTaskReward(Code.FAIL);
        res.taskId = taskId;
        SimCoopTaskData data = ctx.getSimCoopTaskData();
        if (data == null) {
            res.code = Code.NOT_FOUND;
            log.warn("多人任务领奖失败,数据缺失 playerId={},taskId={}", ctx.playerId(), taskId);
            return res;
        }
        SimCoopTaskEntry entry = data.getTasks().get(taskId);
        if (entry == null || entry.getStatus() != CoopTaskConst.TaskStatus.REWARDABLE) {
            log.warn("多人任务领奖失败,状态不符 playerId={},taskId={},status={}",
                    ctx.playerId(), taskId, entry == null ? null : entry.getStatus());
            return res;
        }
        TaskCfg cfg = GameDataManager.getTaskCfg(taskId);
        if (cfg == null) {
            log.warn("多人任务领奖失败,任务配置缺失 playerId={},taskId={}", ctx.playerId(), taskId);
            return res;
        }
        if (cfg.getGetItem() != null && !cfg.getGetItem().isEmpty()) {
            CommonResult<?> addResult = playerPackService.addItems(ctx.playerId(), cfg.getGetItem(),
                    AddType.SIM_COOP_TASK_REWARD);
            if (addResult == null || !addResult.success()) {
                log.error("多人任务领奖失败,道具发放失败 playerId={},taskId={},result={}",
                        ctx.playerId(), taskId, addResult);
                return res;
            }
        }
        //领取后任务直接移除列表 (含今日池位)
        data.getTasks().remove(taskId);
        data.getPoolTaskIds().remove(Integer.valueOf(taskId));
        //领奖后尽快落库, 收窄崩溃重复领取窗口 (对齐 SimTaskService.claimReward)
        ctx.setLastSaveTime(0);

        res.code = Code.SUCCESS;
        log.info("多人任务领奖成功 playerId={},taskId={}", ctx.playerId(), taskId);
        return res;
    }

    // =====================================================================
    // 房间联动 (由路由层/结算回写调用)
    // =====================================================================

    /**
     * 房间创建成功: CLAIMED -> IN_ROOM。
     */
    public void markRoomCreated(SimPlayerContext ctx, int taskId, long roomId, int gameType) {
        SimCoopTaskData data = ctx.getSimCoopTaskData();
        SimCoopTaskEntry entry = data == null ? null : data.getTasks().get(taskId);
        if (entry == null) {
            return;
        }
        int oldStatus = entry.getStatus();
        long oldRoomId = entry.getRoomId();
        int oldGameType = entry.getGameType();
        entry.setStatus(CoopTaskConst.TaskStatus.IN_ROOM);
        entry.setRoomId(roomId);
        entry.setGameType(gameType);
        try {
            coopTaskDao.save(data);
        } catch (RuntimeException e) {
            entry.setStatus(oldStatus);
            entry.setRoomId(oldRoomId);
            entry.setGameType(oldGameType);
            throw e;
        }
        ctx.setLastSaveTime(0);
    }

    /**
     * 获取玩家当前绑定的协作房间，供断线重连后重新路由。
     */
    public CoopTaskInfo getBoundRoomInfo(long playerId) {
        long roomId = roomRecordDao.getPlayerRoom(playerId);
        if (roomId <= 0) {
            return null;
        }
        CoopRoomRecord record = roomRecordDao.get(roomId);
        if (record == null || record.getStatus() == CoopTaskConst.RoomStatus.FINISHED) {
            roomRecordDao.releasePlayerRoom(playerId, roomId);
            return null;
        }

        CoopTaskInfo info = new CoopTaskInfo();
        info.taskId = record.getTaskId();
        info.status = CoopTaskConst.TaskStatus.IN_ROOM;
        info.roomId = roomId;
        info.gameType = record.getGameType();
        return info;
    }

    /**
     * 结算回写 (slots 房间结束经 RPC 调用, 发起者可能已离线)。
     * <p>
     * 发起者: 成功时 IN_ROOM -> REWARDABLE，失败时从已领取任务中删除；协助者: 成功时奖励经邮件发放
     * ("协助者通过其它形式领取奖励", 无贡献门槛)。
     *
     * @param ctx       发起者在线时的上下文 (离线为 null, 直接读写 DB)
     * @param ownerId   发起者
     * @param taskId    任务配置id
     * @param roomId    结算所属房间id，用于拒绝迟到的旧房间回写
     * @param success   任务是否完成
     * @param helperIds 协助者 (不含发起者)
     */
    public boolean onSettle(SimPlayerContext ctx, long ownerId, int taskId, long roomId,
                            boolean success, List<Long> helperIds) {
        int status = success ? CoopTaskConst.TaskStatus.REWARDABLE : CoopTaskConst.TaskStatus.FAILED;
        long finishTime = System.currentTimeMillis();
        boolean firstSettle = coopTaskDao.settleEntryIfInRoom(ownerId, taskId, roomId, status, finishTime);
        if (!firstSettle && !coopTaskDao.isEntrySettled(ownerId, taskId, roomId, status)) {
            log.warn("多人任务结算拒绝,任务状态或房间不匹配 ownerId={},taskId={},roomId={},success={}",
                    ownerId, taskId, roomId, success);
            return false;
        }
        if (ctx != null && ctx.getSimCoopTaskData() != null) {
            SimCoopTaskData data = ctx.getSimCoopTaskData();
            data.getSettlementReceipts().putIfAbsent(roomId,
                    new CoopSettlementReceipt(taskId, status, finishTime));
            SimCoopTaskEntry entry = data.getTasks().get(taskId);
            if (entry != null && entry.getRoomId() == roomId
                    && (entry.getStatus() == CoopTaskConst.TaskStatus.IN_ROOM || entry.getStatus() == status)) {
                entry.setStatus(status);
                entry.setFinishTime(finishTime);
                CoopTaskInfo settledTask = toInfo(taskId, entry);
                if (!success) {
                    data.getTasks().remove(taskId);
                }
                ctx.setLastSaveTime(0);
                if (firstSettle && ctx.getPlayerController() != null) {
                    NotifyCoopTaskUpdate notify = new NotifyCoopTaskUpdate(Code.SUCCESS);
                    notify.task = settledTask;
                    ctx.send(notify);
                }
            }
        }

        //协助者奖励按 roomId+helperId 幂等；即使 DB 已先成功而进程随后崩溃，重试也能补齐未发送邮件。
        //TODO 待策划提供独立协助奖励字段/邮件模板, 当前同任务奖励
        if (success && helperIds != null && !helperIds.isEmpty()) {
            TaskCfg cfg = GameDataManager.getTaskCfg(taskId);
            if (cfg == null || cfg.getGetItem() == null || cfg.getGetItem().isEmpty()) {
                log.error("多人任务协助奖励配置缺失 taskId={},roomId={}", taskId, roomId);
                return false;
            } else {
                List<Item> items = toItemList(cfg.getGetItem());
                for (Long helperId : helperIds) {
                    if (helperId == null || helperId == ownerId) {
                        continue;
                    }
                    try {
                        String bizKey = "coopAssist:" + roomId + ":" + helperId;
                        mailService.addMailIfAbsent(helperId, "多人任务协助奖励",
                                "感谢协助完成多人任务，奖励已发放，请查收。",
                                items, AddType.SIM_COOP_ASSIST_REWARD, bizKey);
                    } catch (Exception e) {
                        log.error("多人任务协助奖励邮件发送失败 helperId={},taskId={}", helperId, taskId, e);
                        return false;
                    }
                }
            }
        }
        log.info("多人任务结算 ownerId={},taskId={},roomId={},success={},firstSettle={},helpers={}",
                ownerId, taskId, roomId, success, firstSettle, helperIds);
        return true;
    }

    /**
     * 自愈: IN_ROOM 但房间不可达时回退待建房态, 避免任务卡死。
     * 不可达 = 记录不存在 (正常结算已删/TTL 过期) 或记录所指 slots 节点已下线 (节点崩溃未恢复)。
     */
    private void selfHealRooms(SimPlayerContext ctx, SimCoopTaskData data) {
        for (SimCoopTaskEntry entry : new ArrayList<>(data.getTasks().values())) {
            if (entry.getStatus() != CoopTaskConst.TaskStatus.IN_ROOM || entry.getRoomId() <= 0) {
                continue;
            }
            try {
                CoopRoomRecord record = roomRecordDao.get(entry.getRoomId());
                if (record != null && record.getStatus() == CoopTaskConst.RoomStatus.FINISHED) {
                    boolean settled = onSettle(ctx, data.getPlayerId(), entry.getTaskId(), entry.getRoomId(),
                            record.isSuccess(), record.getSettlementHelperIds());
                    if (settled) {
                        releaseRoomRecord(data.getPlayerId(), entry.getRoomId(), record);
                    }
                    continue;
                }
                if (record == null || marsCurator.getMarsNode(record.getNodePath()) == null) {
                    log.warn("多人任务房间不可达,回退待建房 playerId={},taskId={},roomId={},node={}",
                            data.getPlayerId(), entry.getTaskId(), entry.getRoomId(),
                            record == null ? null : record.getNodePath());
                    releaseRoomRecord(data.getPlayerId(), entry.getRoomId(), record);
                    entry.setStatus(CoopTaskConst.TaskStatus.CLAIMED);
                    entry.setRoomId(0);
                    entry.setGameType(0);
                    ctx.setLastSaveTime(0);
                }
            } catch (Exception e) {
                //Redis 故障时保持现状, 下次列表再检查
                log.error("多人任务房间自愈检查失败 playerId={},roomId={}", data.getPlayerId(), entry.getRoomId(), e);
            }
        }
    }

    /**
     * 释放协作房间路由记录: 解绑全部成员的"玩家-房间"映射并删除记录; record 为 null 时仅解绑发起者。
     */
    private void releaseRoomRecord(long playerId, long roomId, CoopRoomRecord record) {
        if (record != null) {
            for (Long memberId : record.getMemberIds()) {
                if (memberId != null) {
                    roomRecordDao.releasePlayerRoom(memberId, record.getRoomId());
                }
            }
            roomRecordDao.delete(record.getRoomId());
        } else {
            roomRecordDao.releasePlayerRoom(playerId, roomId);
        }
    }

    // =====================================================================
    // 工具
    // =====================================================================

    private CoopTaskInfo toInfo(int taskId, SimCoopTaskEntry entry) {
        CoopTaskInfo info = new CoopTaskInfo();
        info.taskId = taskId;
        if (entry != null) {
            info.status = entry.getStatus();
            info.roomId = entry.getRoomId();
            info.gameType = entry.getGameType();
        }
        return info;
    }

    private static List<Item> toItemList(Map<Integer, Long> items) {
        List<Item> list = new ArrayList<>(items.size());
        items.forEach((id, count) -> list.add(new Item(id, count)));
        return list;
    }

    private Player resolvePlayer(SimPlayerContext ctx) {
        if (ctx.getPlayerController() != null && ctx.getPlayerController().getPlayer() != null) {
            return ctx.getPlayerController().getPlayer();
        }
        return corePlayerService.get(ctx.playerId());
    }

    static int todayKey() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return today.getYear() * 10000 + today.getMonthValue() * 100 + today.getDayOfMonth();
    }

    static long nextMidnightMillis() {
        return LocalDate.now(ZoneId.systemDefault()).plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public ResCombatPower combatPowers(SimPlayerContext ctx, long playerId) {
        ResCombatPower res = new ResCombatPower(Code.SUCCESS);
        long targetPlayerId = playerId > 0 ? playerId : ctx.playerId();
        SimPlayerContext targetCtx = targetPlayerId == ctx.playerId()
                ? ctx : simPlayerContextRegistry.getContext(targetPlayerId);
        Collection<SimSkillsData> skillsDataList;
        if (targetCtx == null) {
            skillsDataList = simSkillsDao.findByPlayerId(targetPlayerId);
        } else {
            skillsDataList = targetCtx.getSkillsDataMap() == null
                    ? List.of() : targetCtx.getSkillsDataMap().values();
        }
        if (skillsDataList == null || skillsDataList.isEmpty()) {
            return res;
        }

        res.combatPowers = new ArrayList<>();
        for (SimSkillsData skillsData : skillsDataList) {
            Map<Integer, Integer> skillsMap = skillsData.getSkillsMap();
            if (skillsMap == null || skillsMap.isEmpty()) {
                continue;
            }

            KVInfo kvInfo = new KVInfo();
            kvInfo.key = skillsData.getGameType();

            for (Map.Entry<Integer, Integer> en2 : skillsMap.entrySet()) {
                ResearchSkillsCfg cfg = simSkillService.getResearchSkillsCfg(skillsData.getGameType(), en2.getKey(), en2.getValue());
                if (cfg != null) {
                    kvInfo.value += cfg.getCombatPower();
                }
            }
            res.combatPowers.add(kvInfo);
        }
        return res;
    }
}
