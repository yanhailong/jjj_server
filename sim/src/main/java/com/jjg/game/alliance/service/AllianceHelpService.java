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
import com.jjg.game.alliance.pb.res.ResOneKeyHelp;
import com.jjg.game.alliance.pb.res.ResAllianceSeekHelp;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.social.service.SocialSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 *   <li>订单超时惰性清理, 不会无界膨胀。</li>
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
    private AllianceConfigService configService;
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

    // =====================================================================
    // 求助
    // =====================================================================

    /**
     * 发起求助。任务求助要求目标是我当前接取的任务; 建筑加速目标为升级中的建筑
     * (建筑状态由消费端校验, 无效订单自然无效果)。
     */
    public ResAllianceSeekHelp seekHelp(long playerId, int type, long targetId, String targetName) {
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
        int seeked = playerData.seekHelpCountOf(today);
        if (seeked >= AllianceConst.Cfg.DAILY_SEEK_HELP_LIMIT) {
            res.code = Code.FORBID;
            log.warn("发起联盟求助失败,今日求助次数已达上限 playerId={},seeked={},limit={}", playerId, seeked, AllianceConst.Cfg.DAILY_SEEK_HELP_LIMIT);
            return res;
        }

        int maxHelp;
        if (type == AllianceConst.HelpType.TASK) {
            PlayerTakenTask taken = playerData.getTakenTask();
            if (taken == null || taken.getUid() != targetId) {
                res.code = Code.NOT_FOUND;
                log.warn("发起联盟求助失败,目标任务非当前接取任务 playerId={},targetId={},takenUid={}", playerId, targetId, taken == null ? 0 : taken.getUid());
                return res;
            }
            AllianceConfigService.TaskCfg cfg = configService.taskCfg(taken.getCfgId());
            maxHelp = cfg == null ? AllianceConst.Cfg.TASK_ORDER_MAX_HELP : cfg.maxHelp();
        } else {
            maxHelp = AllianceConst.Cfg.SPEEDUP_ORDER_MAX_HELP;
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
        if (!alliancePlayerDao.tryConsumeSeekHelp(playerId, today, AllianceConst.Cfg.DAILY_SEEK_HELP_LIMIT)) {
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
        res.remainSeek = Math.max(0, AllianceConst.Cfg.DAILY_SEEK_HELP_LIMIT - latest.seekHelpCountOf(today));
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
        int code = doHelp(playerId, allianceId, order, today);
        if (code != Code.SUCCESS) {
            res.code = code;
            log.warn("联盟帮助失败 helper={},allianceId={},orderId={},code={}", playerId, allianceId, orderId, code);
            return res;
        }
        res.rewardContribution = AllianceConst.Cfg.HELP_REWARD_CONTRIBUTION;
        AlliancePlayerData latest = alliancePlayerDao.getOrEmpty(playerId);
        res.remainHelp = Math.max(0, AllianceConst.Cfg.DAILY_HELP_LIMIT - latest.helpCountOf(today));
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
        int remain = AllianceConst.Cfg.DAILY_HELP_LIMIT - helped;
        if (remain <= 0) {
            res.code = Code.FORBID;
            log.warn("联盟一键帮助失败,今日帮助次数已达上限 playerId={},helped={},limit={}", playerId, helped, AllianceConst.Cfg.DAILY_HELP_LIMIT);
            return res;
        }
        long now = System.currentTimeMillis();
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
            if (order.helpedBy(playerId) || order.full()) {
                res.skippedCount++;
                continue;
            }
            int code = doHelp(playerId, allianceId, order, today);
            if (code == Code.SUCCESS) {
                res.helpedCount++;
                remain--;
            } else {
                res.skippedCount++;
                if (code == Code.FORBID) {
                    break;
                }
            }
        }
        res.rewardContribution = (long) res.helpedCount * AllianceConst.Cfg.HELP_REWARD_CONTRIBUTION;
        res.remainHelp = remain;
        return res;
    }

    /**
     * 帮助执行: 条件更新占坑 -> 按类型生效 -> 奖励贡献值 -> 通知求助者。
     */
    private int doHelp(long playerId, long allianceId, AllianceHelpOrder order, int today) {
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
        if (!alliancePlayerDao.tryConsumeHelp(playerId, today, AllianceConst.Cfg.DAILY_HELP_LIMIT)) {
            return Code.FORBID;
        }
        if (!allianceDao.tryHelp(allianceId, order.getOrderId(), playerId, now, order.getMaxHelp())) {
            alliancePlayerDao.rollbackHelp(playerId, today);
            return Code.REPEAT_OP;
        }
        cacheService.publishInvalidate(allianceId);

        long notifyValue;
        if (order.getType() == AllianceConst.HelpType.TASK) {
            taskService.onTaskHelped(order.getOwnerId(), order.getTargetId());
            notifyValue = 1;
        } else {
            String key = speedupKey(order.getOwnerId(), order.getTargetId());
            stringRedisTemplate.opsForValue().increment(key, AllianceConst.Cfg.SPEEDUP_SEC_PER_HELP);
            stringRedisTemplate.expire(key, AllianceConst.Cfg.SPEEDUP_TTL_SEC, TimeUnit.SECONDS);
            notifyValue = AllianceConst.Cfg.SPEEDUP_SEC_PER_HELP;
        }

        assetService.grantContribution(playerId, AllianceConst.Cfg.HELP_REWARD_CONTRIBUTION, 0, allianceId);

        Player helper = corePlayerService.get(playerId);
        NotifyAllianceHelped notify = new NotifyAllianceHelped(Code.SUCCESS);
        notify.orderId = order.getOrderId();
        notify.type = order.getType();
        notify.helperId = playerId;
        notify.helperNick = helper == null ? "" : helper.getNickName();
        notify.value = notifyValue;
        socialSender.sendTo(order.getOwnerId(), notify);

        allianceDao.removeHelpOrderIfFull(allianceId, order.getOrderId(), order.getMaxHelp());
        log.info("联盟帮助成功 helper={},owner={},orderId={},type={}", playerId, order.getOwnerId(),
                order.getOrderId(), order.getType());
        return Code.SUCCESS;
    }

    @Deprecated
    private int doHelpLegacy(long playerId, long allianceId, AllianceHelpOrder order, int today, int helpedBefore) {
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
        //原子占坑: 每单每人一次
        if (!allianceDao.tryHelp(allianceId, order.getOrderId(), playerId, now)) {
            return Code.REPEAT_OP;
        }
        cacheService.publishInvalidate(allianceId);
        alliancePlayerDao.setHelp(playerId, today, helpedBefore + 1);

        //帮助生效
        long notifyValue;
        boolean orderDone;
        if (order.getType() == AllianceConst.HelpType.TASK) {
            //任务求助: 帮助即完成对方任务 (上限1次)
            taskService.onTaskHelped(order.getOwnerId(), order.getTargetId());
            notifyValue = 1;
            orderDone = true;
        } else {
            //建筑加速: 抵扣秒数入 Redis, 由对方节点消费
            String key = speedupKey(order.getOwnerId(), order.getTargetId());
            stringRedisTemplate.opsForValue().increment(key, AllianceConst.Cfg.SPEEDUP_SEC_PER_HELP);
            stringRedisTemplate.expire(key, AllianceConst.Cfg.SPEEDUP_TTL_SEC, TimeUnit.SECONDS);
            notifyValue = AllianceConst.Cfg.SPEEDUP_SEC_PER_HELP;
            orderDone = order.helpedCount() + 1 >= order.getMaxHelp();
        }
        //帮助奖励贡献值 (不计贡献度——贡献度口径为给联盟的声誉)
        assetService.grantContribution(playerId, AllianceConst.Cfg.HELP_REWARD_CONTRIBUTION, 0, allianceId);

        //通知求助者
        Player helper = corePlayerService.get(playerId);
        NotifyAllianceHelped notify = new NotifyAllianceHelped(Code.SUCCESS);
        notify.orderId = order.getOrderId();
        notify.type = order.getType();
        notify.helperId = playerId;
        notify.helperNick = helper == null ? "" : helper.getNickName();
        notify.value = notifyValue;
        socialSender.sendTo(order.getOwnerId(), notify);

        //订单完成则移除
        if (orderDone) {
            allianceDao.removeHelpOrders(allianceId, List.of(order.getOrderId()));
            cacheService.publishInvalidate(allianceId);
        }
        log.info("联盟帮助 helper={},owner={},orderId={},type={}", playerId, order.getOwnerId(),
                order.getOrderId(), order.getType());
        return Code.SUCCESS;
    }

    // =====================================================================
    // 列表
    // =====================================================================

    /**
     * 求助订单列表 (顺带惰性清理超时订单)。
     */
    public ResAllianceHelpList helpList(long playerId) {
        ResAllianceHelpList res = new ResAllianceHelpList(Code.SUCCESS);
        res.orders = new ArrayList<>();
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
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
        res.dailyHelpLimit = AllianceConst.Cfg.DAILY_HELP_LIMIT;
        res.remainHelp = Math.max(0, AllianceConst.Cfg.DAILY_HELP_LIMIT - playerData.helpCountOf(today));
        return res;
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

    private boolean expired(AllianceHelpOrder order, long now) {
        return now - order.getCreateTime() > AllianceConst.Cfg.HELP_ORDER_VALID_MILLS;
    }

    private String speedupKey(long playerId, long buildingId) {
        return AllianceConst.RedisKey.SPEEDUP_PREFIX + playerId + ":" + buildingId;
    }
}
