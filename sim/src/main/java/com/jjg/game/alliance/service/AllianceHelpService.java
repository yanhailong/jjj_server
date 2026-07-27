package com.jjg.game.alliance.service;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AllianceDao;
import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AllianceHelpOrder;
import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.data.PlayerTakenTask;
import com.jjg.game.alliance.pb.AlliancePbConverter;
import com.jjg.game.alliance.pb.res.NotifyAllianceHelped;
import com.jjg.game.alliance.pb.res.ResAllianceHelp;
import com.jjg.game.alliance.pb.res.ResAllianceHelpList;
import com.jjg.game.alliance.pb.res.ResGetHelpInfo;
import com.jjg.game.alliance.pb.res.ResOneKeyHelp;
import com.jjg.game.alliance.pb.res.ResAllianceSeekHelp;
import com.jjg.game.alliance.pb.struct.AllianceHelpOrderInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.service.SimConfigCacheService;
import com.jjg.game.social.service.SocialSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 成员互助: 求助(任务/建筑加速) + 帮助 + 一键帮助。
 * <p>
 * 关键设计:
 * <ul>
 *   <li>求助订单内嵌联盟文档, "每单每人一次"由 helpers 存在性条件更新保证, 多节点并发安全;</li>
 *   <li>建筑加速不跨节点写对方内存: 帮助产生的抵扣秒数累计在 Redis
 *       ({@code alliance:help:speedup:{pid}:{buildingId}}), 由被帮助者所在节点在
 *       建筑查询/完成时调用 {@link #consumeSpeedupSeconds} 消费抵扣, 同时实时 NOTIFY 求助者刷新;</li>
 *   <li>任务求助帮助即完成任务 (上限 1 次, 见 AllianceTaskService.onTaskHelped);</li>
 *   <li>订单超时惰性清理, 不会无界膨胀;</li>
 *   <li>建筑加速数值取 global 表 225(每日被帮助上限)/226(每日给出帮助上限)/
 *       227(每次减少分钟)/228(每次帮助贡献值)/249(每日分享=发起加速求助上限),
 *       见 {@link AllianceConst.Global}; 任务求助每日上限为固定值, 与分享独立计数。</li>
 * </ul>
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceHelpService {
    private static final Logger log = LoggerFactory.getLogger(AllianceHelpService.class);

    @Autowired
    private AllianceDao allianceDao;
    @Autowired
    private AlliancePlayerDao alliancePlayerDao;
    @Autowired
    private AllianceCacheService cacheService;
    @Autowired
    private SimConfigCacheService configService;
    @Autowired
    private AllianceAssetService assetService;
    @Autowired
    private AllianceTaskService taskService;
    @Autowired
    private SnowflakeManager snowflakeManager;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SocialSender socialSender;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;

    // =====================================================================
    // 求助
    // =====================================================================

    /**
     * 发起求助。任务求助要求目标是我当前接取的任务; 建筑加速目标为升级中的建筑
     * (建筑状态由消费端校验, 无效订单自然无效果)。
     */
    public ResAllianceSeekHelp seekHelp(long playerId, int type, int targetId, String targetName) {
        ResAllianceSeekHelp res = new ResAllianceSeekHelp(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("发起联盟求助失败,玩家不在联盟 playerId={},type={},targetId={}", playerId, type, targetId);
            return res;
        }
        if (type != AllianceConst.HelpType.TASK && type != AllianceConst.HelpType.BUILD_SPEEDUP) {
            res.code = Code.PARAM_ERROR;
            log.warn("发起联盟求助失败,求助类型非法 playerId={},type={}", playerId, type);
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();
        //每日求助限额按类型独立: 任务=固定值, 建筑加速(分享)=global 249
        boolean speedup = type == AllianceConst.HelpType.BUILD_SPEEDUP;
        int seekLimit = speedup
                ? globalInt(AllianceConst.Global.SPEEDUP_DAILY_SEEK_LIMIT_ID)
                : AllianceConst.Cfg.DAILY_SEEK_HELP_LIMIT;
        int seeked = speedup ? playerData.speedupSeekCountOf(today) : playerData.seekHelpCountOf(today);
        if (seeked >= seekLimit) {
            res.code = Code.FORBID;
            log.warn("发起联盟求助失败,今日求助次数已达上限 playerId={},type={},seeked={},limit={}", playerId, type, seeked, seekLimit);
            return res;
        }

        int maxHelp;
        if (type == AllianceConst.HelpType.TASK) {
            PlayerTakenTask taken = playerData.getTakenTask();
            if (taken == null || taken.getCfgId() != targetId) {
                res.code = Code.NOT_FOUND;
                log.warn("发起联盟求助失败,目标任务非当前接取任务 playerId={},targetId={},takenCfgId={}", playerId, targetId, taken == null ? 0 : taken.getCfgId());
                return res;
            }
            maxHelp = AllianceConst.Cfg.TASK_ORDER_MAX_HELP;
        } else {
            //目标必须是本人正在升级CD中的建筑: 否则帮助只会累计到一个不会被消费的 key,
            //白扣帮助者次数且无任何效果
            if (!upgradingBuilding(playerId, targetId)) {
                res.code = Code.NOT_FOUND;
                log.warn("发起联盟求助失败,目标建筑不在升级CD中 playerId={},targetId={}", playerId, targetId);
                return res;
            }
            //单订单可被帮上限对齐每日被帮助上限 (跨订单的当日总量由 speedupHelped 计数兜底)
            maxHelp = globalInt(AllianceConst.Global.SPEEDUP_DAILY_HELPED_LIMIT_ID);
        }
        //同目标重复求助拦截 (扫当前订单, 订单量有界)
        for (AllianceHelpOrder order : alliance.getHelpOrders().values()) {
            if (order.getOwnerId() == playerId && order.getType() == type && order.getTargetId() == targetId) {
                res.code = Code.REPEAT_OP;
                log.warn("发起联盟求助失败,同目标已有求助订单 playerId={},type={},targetId={},orderId={}", playerId, type, targetId, order.getOrderId());
                return res;
            }
        }

        AllianceHelpOrder order = new AllianceHelpOrder();
        order.setOrderId(snowflakeManager.nextId());
        order.setType(type);
        order.setOwnerId(playerId);
        order.setTargetId(targetId);
        order.setTargetName(targetName == null ? "" : targetName);
        order.setCreateTime(System.currentTimeMillis());
        order.setMaxHelp(maxHelp);
        order.setHelpCount(0);
        boolean consumed = speedup
                ? alliancePlayerDao.tryConsumeSpeedupSeek(playerId, today, seekLimit)
                : alliancePlayerDao.tryConsumeSeekHelp(playerId, today, seekLimit);
        if (!consumed) {
            res.code = Code.FORBID;
            log.warn("发起联盟求助失败,扣减求助次数失败(并发达上限) playerId={},type={},targetId={}", playerId, type, targetId);
            return res;
        }
        allianceDao.addHelpOrder(allianceId, order);
        cacheService.publishInvalidate(allianceId);
        //广播全盟: 有新求助订单 (前端在联盟频道渲染卡片)
        assetService.broadcastToAlliance(allianceId, AllianceConst.NotifyType.NEW_HELP_ORDER,
                String.valueOf(order.getOrderId()));

        Player self = corePlayerService.get(playerId);
        res.order = AlliancePbConverter.toHelpOrderInfo(order, self == null ? "" : self.getNickName(), playerId);
        AlliancePlayerData latest = alliancePlayerDao.getOrEmpty(playerId);
        int latestSeeked = speedup ? latest.speedupSeekCountOf(today) : latest.seekHelpCountOf(today);
        res.remainSeek = Math.max(0, seekLimit - latestSeeked);
        log.info("发起联盟求助 playerId={},allianceId={},type={},targetId={}", playerId, allianceId, type, targetId);
        return res;
    }

    // =====================================================================
    // 帮助
    // =====================================================================

    /**
     * 帮助单个订单 (点击助力)。
     */
    public ResAllianceHelp help(long playerId, long orderId) {
        ResAllianceHelp res = new ResAllianceHelp(Code.SUCCESS);
        res.orderId = orderId;
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("联盟帮助失败,玩家不在联盟 playerId={},orderId={}", playerId, orderId);
            return res;
        }
        int today = TimeHelper.getDayNumerical();
        AllianceHelpOrder order = alliance.getHelpOrders().get(orderId);
        int code = doHelp(playerId, allianceId, order, today, true);
        if (code != Code.SUCCESS) {
            res.code = code;
            log.warn("联盟帮助失败 helper={},allianceId={},orderId={},code={}", playerId, allianceId, orderId, code);
            return res;
        }
        res.rewardContribution = globalInt(AllianceConst.Global.HELP_REWARD_CONTRIBUTION_ID);
        AlliancePlayerData latest = alliancePlayerDao.getOrEmpty(playerId);
        res.remainHelp = Math.max(0, globalInt(AllianceConst.Global.SPEEDUP_DAILY_HELP_LIMIT_ID) - latest.helpCountOf(today));
        return res;
    }

    /**
     * 一键帮助 (仅建筑加速类订单, 需求明确对任务求助无效)。
     * 按"今日剩余帮助次数"与"可帮订单数"取小, 逐单帮助。
     */
    public ResOneKeyHelp oneKeyHelp(long playerId) {
        ResOneKeyHelp res = new ResOneKeyHelp(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("联盟一键帮助失败,玩家不在联盟 playerId={}", playerId);
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();
        int helped = playerData.helpCountOf(today);
        int dailyHelpLimit = globalInt(AllianceConst.Global.SPEEDUP_DAILY_HELP_LIMIT_ID);
        int remain = dailyHelpLimit - helped;
        if (remain <= 0) {
            res.code = Code.FORBID;
            log.warn("联盟一键帮助失败,今日帮助次数已达上限 playerId={},helped={},limit={}", playerId, helped, dailyHelpLimit);
            return res;
        }
        long now = System.currentTimeMillis();
        //当日被帮额度已满的求助者, 其余订单直接跳过, 省去必然失败的库操作
        Set<Long> helpedLimitOwners = new HashSet<>();
        for (AllianceHelpOrder order : new ArrayList<>(alliance.getHelpOrders().values())) {
            if (remain <= 0) {
                break;
            }
            if (order.getType() != AllianceConst.HelpType.BUILD_SPEEDUP) {
                continue;
            }
            if (order.getOwnerId() == playerId || expired(order, now)) {
                continue;
            }
            if (order.helpedBy(playerId) || order.full() || helpedLimitOwners.contains(order.getOwnerId())) {
                res.skippedCount++;
                continue;
            }
            int code = doHelp(playerId, allianceId, order, today, false);
            if (code == Code.SUCCESS) {
                res.helpedCount++;
                remain--;
            } else {
                res.skippedCount++;
                if (code == Code.ALLIANCE_HELPED_LIMIT) {
                    helpedLimitOwners.add(order.getOwnerId());
                } else if (code == Code.FORBID) {
                    break;
                }
            }
        }
        //一键场景合并为一次失效广播, 避免逐单触发全节点缓存重建
        if (res.helpedCount > 0) {
            cacheService.publishInvalidate(allianceId);
        }
        res.rewardContribution = (long) res.helpedCount * globalInt(AllianceConst.Global.HELP_REWARD_CONTRIBUTION_ID);
        res.remainHelp = remain;
        return res;
    }

    /**
     * 帮助执行: 条件更新占坑 -> 按类型生效 -> 奖励贡献值 -> 通知求助者。
     *
     * @param invalidate 成功后是否立即广播缓存失效; 一键帮助传 false, 由调用方循环后统一广播
     */
    private int doHelp(long playerId, long allianceId, AllianceHelpOrder order, int today, boolean invalidate) {
        long now = System.currentTimeMillis();
        if (order == null || expired(order, now)) {
            return Code.NOT_FOUND;
        }
        if (order.getOwnerId() == playerId) {
            return Code.PARAM_ERROR;
        }
        if (order.helpedBy(playerId) || order.full()) {
            return Code.REPEAT_OP;
        }
        //每日帮助限额(226)只约束建筑加速; 任务帮助由"单订单限1人 + 求助方每日限额"自然有界
        boolean speedup = order.getType() == AllianceConst.HelpType.BUILD_SPEEDUP;
        if (speedup) {
            if (!alliancePlayerDao.tryConsumeHelp(playerId, today, globalInt(AllianceConst.Global.SPEEDUP_DAILY_HELP_LIMIT_ID))) {
                return Code.FORBID;
            }
            //占用求助者"当日被帮助"额度 (global 225, 跨订单共享)
            if (!alliancePlayerDao.tryConsumeSpeedupHelped(order.getOwnerId(), today,
                    globalInt(AllianceConst.Global.SPEEDUP_DAILY_HELPED_LIMIT_ID))) {
                alliancePlayerDao.rollbackHelp(playerId, today);
                return Code.ALLIANCE_HELPED_LIMIT;
            }
        }
        if (!allianceDao.tryHelp(allianceId, order.getOrderId(), playerId, now, order.getMaxHelp())) {
            if (speedup) {
                alliancePlayerDao.rollbackHelp(playerId, today);
                alliancePlayerDao.rollbackSpeedupHelped(order.getOwnerId(), today);
            }
            return Code.REPEAT_OP;
        }

        long notifyValue;
        if (order.getType() == AllianceConst.HelpType.TASK) {
            taskService.onTaskHelped(order.getOwnerId(), (int) order.getTargetId());
            notifyValue = 1;
        } else {
            long speedupSeconds = globalInt(AllianceConst.Global.SPEEDUP_MINUTES_PER_HELP_ID) * 60L;
            String key = speedupKey(order.getOwnerId(), order.getTargetId());
            stringRedisTemplate.opsForValue().increment(key, speedupSeconds);
            stringRedisTemplate.expire(key, AllianceConst.Cfg.SPEEDUP_TTL_SEC, TimeUnit.SECONDS);
            notifyValue = speedupSeconds;
        }

        assetService.grantContribution(playerId, globalInt(AllianceConst.Global.HELP_REWARD_CONTRIBUTION_ID), 0, allianceId);

        Player helper = corePlayerService.get(playerId);
        NotifyAllianceHelped notify = new NotifyAllianceHelped(Code.SUCCESS);
        notify.orderId = order.getOrderId();
        notify.type = order.getType();
        notify.helperId = playerId;
        notify.helperNick = helper == null ? "" : helper.getNickName();
        notify.value = notifyValue;
        socialSender.sendTo(order.getOwnerId(), notify);

        //全盟推送最新订单详情, 供聊天卡片/列表刷新 (含本单刚完成帮助后的进度)
        broadcastGetHelpInfo(allianceId, order, playerId, now);

        allianceDao.removeHelpOrderIfFull(allianceId, order.getOrderId(), order.getMaxHelp());
        //失效放在订单移除之后, 一次广播覆盖占坑与移除两处变更
        if (invalidate) {
            cacheService.publishInvalidate(allianceId);
        }
        log.info("联盟帮助成功 helper={},owner={},orderId={},type={}", playerId, order.getOwnerId(),
                order.getOrderId(), order.getType());
        return Code.SUCCESS;
    }

    /**
     * 帮助成功后向全盟成员推送 {@link ResGetHelpInfo}, 每人按自身计算 myHelped。
     * 本地缓存尚未失效时 helpers 可能滞后, 用本次帮助结果叠一份快照再下发。
     */
    private void broadcastGetHelpInfo(long allianceId, AllianceHelpOrder order, long helperId, long helpTime) {
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null || alliance.getMembers() == null || alliance.getMembers().isEmpty()) {
            return;
        }
        AllianceHelpOrder view = snapshotAfterHelp(order, helperId, helpTime);
        Player owner = corePlayerService.get(view.getOwnerId());
        String ownerNick = owner == null ? "" : owner.getNickName();
        for (Long memberId : alliance.getMembers().keySet()) {
            if (memberId == null) {
                continue;
            }
            ResGetHelpInfo res = new ResGetHelpInfo(Code.SUCCESS);
            res.helpOrderInfo = AlliancePbConverter.toHelpOrderInfo(view, ownerNick, memberId);
            socialSender.sendTo(memberId, res);
        }
    }

    /**
     * 构造"本次帮助已计入"的订单快照, 不改动缓存中的原对象。
     */
    private static AllianceHelpOrder snapshotAfterHelp(AllianceHelpOrder order, long helperId, long helpTime) {
        AllianceHelpOrder view = new AllianceHelpOrder();
        view.setOrderId(order.getOrderId());
        view.setType(order.getType());
        view.setOwnerId(order.getOwnerId());
        view.setTargetId(order.getTargetId());
        view.setTargetName(order.getTargetName());
        view.setCreateTime(order.getCreateTime());
        view.setMaxHelp(order.getMaxHelp());
        Map<Long, Long> helpers = order.getHelpers() == null
                ? new HashMap<>()
                : new HashMap<>(order.getHelpers());
        helpers.put(helperId, helpTime);
        view.setHelpers(helpers);
        view.setHelpCount(Math.max(order.getHelpCount() + 1, helpers.size()));
        return view;
    }

    // =====================================================================
    // 列表 / 详情
    // =====================================================================

    /**
     * 按 orderId 查询单条求助订单详情 (聊天卡片刷新等场景)。
     * 订单不存在、已过期或玩家不在联盟时返回 NOT_FOUND。
     */
    public ResGetHelpInfo getHelpInfo(long playerId, long orderId) {
        ResGetHelpInfo res = new ResGetHelpInfo(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("获取求助信息失败，未找到联盟信息 playerId={},allianceId={}", playerId, allianceId);
            return res;
        }
        AllianceHelpOrder order = alliance.getHelpOrders().get(orderId);
        if (order == null || expired(order, System.currentTimeMillis())) {
            res.code = Code.NOT_FOUND;
            res.helpOrderInfo = new AllianceHelpOrderInfo();
            res.helpOrderInfo.orderId = orderId;
            log.warn("获取求助信息失败，未找到求助信息 playerId={},allianceId={}", playerId, allianceId);
            return res;
        }
        Player owner = corePlayerService.get(order.getOwnerId());
        res.helpOrderInfo = AlliancePbConverter.toHelpOrderInfo(order,
                owner == null ? "" : owner.getNickName(), playerId);
        return res;
    }

    /**
     * 求助订单列表 (顺带惰性清理超时订单)。
     */
    public ResAllianceHelpList helpList(long playerId) {
        ResAllianceHelpList res = new ResAllianceHelpList(Code.SUCCESS);
        res.orders = new ArrayList<>();
        long allianceId = cacheService.getAllianceId(playerId);
        if (allianceId < 1) {
            res.code = Code.NOT_FOUND;
            log.warn("获取求助订单失败，玩家不在联盟 playerId={}", playerId);
            return res;
        }
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("获取求助订单失败，未找到玩家所在联盟 playerId={},allianceId={}", playerId, allianceId);
            return res;
        }
        long now = System.currentTimeMillis();
        List<Long> expiredIds = new ArrayList<>();
        List<AllianceHelpOrder> valid = new ArrayList<>();
        for (AllianceHelpOrder order : alliance.getHelpOrders().values()) {
            if (expired(order, now)) {
                expiredIds.add(order.getOrderId());
            } else {
                valid.add(order);
            }
        }
        if (!expiredIds.isEmpty()) {
            allianceDao.removeHelpOrders(allianceId, expiredIds);
            cacheService.publishInvalidate(allianceId);
        }
        //批量取求助者昵称
        List<Long> ownerIds = valid.stream().map(AllianceHelpOrder::getOwnerId).distinct().toList();
        Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(ownerIds);
        valid.sort((a, b) -> Long.compare(b.getCreateTime(), a.getCreateTime()));
        for (AllianceHelpOrder order : valid) {
            Player owner = playerMap.get(order.getOwnerId());
            res.orders.add(AlliancePbConverter.toHelpOrderInfo(order,
                    owner == null ? "" : owner.getNickName(), playerId));
        }

        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();
        res.dailySeekLimit = AllianceConst.Cfg.DAILY_SEEK_HELP_LIMIT;
        res.remainSeek = Math.max(0, AllianceConst.Cfg.DAILY_SEEK_HELP_LIMIT - playerData.seekHelpCountOf(today));
        SpeedupQuota quota = speedupQuota(playerData);
        res.dailyShareLimit = quota.dailyShareLimit();
        res.remainShare = quota.remainShare();
        res.dailyHelpLimit = quota.dailyHelpLimit();
        res.remainHelp = quota.remainHelp();
        return res;
    }

    /**
     * 建筑加速的 帮助/分享(发起加速求助) 每日上限及今日剩余次数。
     */
    public record SpeedupQuota(int remainHelp, int dailyHelpLimit, int remainShare, int dailyShareLimit) {
    }

    public SpeedupQuota speedupQuota(long playerId) {
        return speedupQuota(alliancePlayerDao.getOrEmpty(playerId));
    }

    private SpeedupQuota speedupQuota(AlliancePlayerData playerData) {
        int today = TimeHelper.getDayNumerical();
        int dailyHelpLimit = globalInt(AllianceConst.Global.SPEEDUP_DAILY_HELP_LIMIT_ID);
        int dailyShareLimit = globalInt(AllianceConst.Global.SPEEDUP_DAILY_SEEK_LIMIT_ID);
        return new SpeedupQuota(
                Math.max(0, dailyHelpLimit - playerData.helpCountOf(today)),
                dailyHelpLimit,
                Math.max(0, dailyShareLimit - playerData.speedupSeekCountOf(today)),
                dailyShareLimit);
    }

    // =====================================================================
    // 建筑加速消费 (供 sim 建筑系统接入)
    // =====================================================================

    /**
     * 消费玩家某建筑累计的加速抵扣秒数 (原子取出并清零)。
     * <p>
     * 接入点: SimBuildingService 计算建筑升级剩余时间/完成校验处调用, 把返回秒数从剩余时间中扣除。
     *
     * @return 累计可抵扣秒数 (无则 0)
     */
    public long consumeSpeedupSeconds(long playerId, long buildingId) {
        try {
            String val = stringRedisTemplate.opsForValue().getAndDelete(speedupKey(playerId, buildingId));
            return val == null ? 0 : Long.parseLong(val);
        } catch (Exception e) {
            log.warn("消费建筑加速抵扣失败 playerId={},buildingId={}", playerId, buildingId, e);
            return 0;
        }
    }

    /**
     * 目标建筑是否处于升级 CD。发起求助必然在求助者自己的节点执行, ctx 在本地;
     * 取不到 ctx/场景时放行, 不误伤。
     */
    private boolean upgradingBuilding(long playerId, int buildingId) {
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        if (ctx == null || ctx.getCurrentCasino() == null) {
            return true;
        }
        BuildingData building = ctx.getCurrentCasino().findBuilding(buildingId);
        return building != null && building.isUpgrading(System.currentTimeMillis());
    }

    private boolean expired(AllianceHelpOrder order, long now) {
        return now - order.getCreateTime() > AllianceConst.Cfg.HELP_ORDER_VALID_MILLS;
    }

    /**
     * 读 global 表整型配置; 缺失时告警并返回 0 (上限/数值类配置取 0 即关闭对应行为, fail-closed)。
     */
    private int globalInt(int id) {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(id);
        if (cfg == null) {
            log.error("global 配置缺失 id={}", id);
            return 0;
        }
        return cfg.getIntValue();
    }

    private String speedupKey(long playerId, long buildingId) {
        return AllianceConst.RedisKey.SPEEDUP_PREFIX + playerId + ":" + buildingId;
    }
}
