package com.jjg.game.sim.service;

import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.constant.SimVisitConstant;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.dao.SimTaskDao;
import com.jjg.game.sim.dao.SimVisitDao;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.pb.res.ResDeleteVisitComment;
import com.jjg.game.sim.pb.res.ResVisitAction;
import com.jjg.game.sim.pb.res.ResVisitCasino;
import com.jjg.game.sim.pb.res.ResVisitComments;
import com.jjg.game.sim.pb.res.ResVisitRank;
import com.jjg.game.sim.pb.res.ResVisitRankRewards;
import com.jjg.game.sim.pb.res.ResVisitRecords;
import com.jjg.game.sim.pb.res.ResVisitSummary;
import com.jjg.game.sim.pb.res.ResVisitTrial;
import com.jjg.game.sim.pb.struct.VisitBuildingInfo;
import com.jjg.game.sim.pb.struct.VisitCasinoInfo;
import com.jjg.game.sim.pb.struct.VisitCommentInfo;
import com.jjg.game.sim.pb.struct.VisitGameInfo;
import com.jjg.game.sim.pb.struct.VisitRecordInfo;
import com.jjg.game.social.service.SimRobotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 玩家拜访业务编排。
 *
 * @author 11
 * @date 2026/6/30
 */
@Service
public class SimVisitService implements IRedDotService {
    private static final Logger log = LoggerFactory.getLogger(SimVisitService.class);
    private static final int RANDOM_SAMPLE_SIZE = 3;
    private static final long RANDOM_VISIT_MIN_INTERVAL_MILLIS = 1000;

    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private SimCasinoDao simCasinoDao;
    @Autowired
    private SimPlayerGameDao simPlayerGameDao;
    @Autowired
    private SimSkillsDao simSkillsDao;
    @Autowired
    private SimTaskDao simTaskDao;
    @Autowired
    private SimVisitDao visitDao;
    @Autowired
    private SimVisitQuotaService quotaService;
    @Autowired
    private SimVisitConfigService configService;
    @Autowired
    private SimVisitRankService rankService;
    @Autowired
    private SimConfigCacheService configCacheService;
    @Autowired
    private SimCasinoService simCasinoService;
    @Autowired
    private SnowflakeManager snowflakeManager;
    @Autowired
    private CountDao countDao;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private AllianceEventService allianceEventService;
    @Autowired
    private RedDotManager redDotManager;
    @Autowired
    private SimRobotService simRobotService;


    public ResVisitCasino visit(SimPlayerContext ctx, long playerId, int casinoId) {
        ResVisitCasino res = new ResVisitCasino(Code.SUCCESS);
        try {
            boolean robot = RobotUtil.isRobot(playerId);
            TargetResult target;
            if (robot) {
                RobotCfg robotCfg = simRobotService.queryRobotCfg(playerId);
                if (robotCfg == null) {
                    log.warn("获取机器人配置失败 selfId={},targetId={}", ctx.playerId(), playerId);
                    res.code = Code.SAMPLE_ERROR;
                    return res;
                }

                long bindPlayerId = simRobotService.queryPlayerId(robotCfg.getId());
                if (bindPlayerId < 1) {
                    bindPlayerId = simPlayerGameDao.findRandomVisitCandidates(2).stream()
                            .mapToLong(candidate -> candidate.getPlayerId())
                            .filter(pid -> pid != ctx.playerId())
                            .findFirst()
                            .orElse(0);
                    if (bindPlayerId > 0) {
                        simRobotService.bind(playerId, bindPlayerId);
                    }
                }
                System.out.println(bindPlayerId);
                target = findTarget(ctx.playerId(), bindPlayerId, casinoId);
            } else {
                target = findTarget(ctx.playerId(), playerId, casinoId);
            }

            if (!target.success()) {
                res.code = target.code();
                return res;
            }
            Map<Long, SimVisitProfileData> profiles = new HashMap<>();
            for (SimVisitProfileData profile : visitDao.findAllByPlayerIds(
                    List.of(ctx.playerId(), playerId))) {
                profiles.put(profile.getPlayerId(), profile);
            }
            int likeLimit = configService.getDailyLikeLimit();
            int commentLimit = configService.getDailyCommentLimit();
            int trialLimit = configService.getDailyTrialLimit();
            SimVisitQuotaService.VisitQuotaSnapshot quota = quotaService.visitSnapshot(
                    ctx.playerId(), playerId, likeLimit, commentLimit, trialLimit);
            res.info = buildCasinoInfo(target.player(), target.casino(), profiles.get(playerId),
                    quota.ownerTodayPopularity());
            //记住当前拜访对象, 留言板等后续请求据此确定查谁的数据
            ctx.setVisitTargetId(playerId);
            quotaService.markVisited(playerId, ctx.playerId());
            quotaService.markTargetVisited(ctx.playerId(), playerId);
            //主线任务: 拜访一次 (含随机拜访) -> 推进 12217
            allianceEventService.onVisit(ctx.playerId());
            res.remainingLikes = quota.remainingLikes();
            res.dailyLikeLimit = likeLimit;
            res.remainingComments = quota.remainingComments();
            res.dailyCommentLimit = commentLimit;
            res.remainingTrials = quota.remainingTrials();
            res.dailyTrialLimit = trialLimit;
            SimVisitProfileData self = profiles.get(ctx.playerId());
            res.unreadComments = self == null ? 0 : self.getUnreadCommentCount();
            res.commentUnlocked = canComment(ctx.playerId());
            res.commissionRate = configService.getCommissionRate();
        } catch (Exception e) {
            log.error("拜访赌场失败 visitorId={},ownerId={},casinoId={}", ctx.playerId(), playerId, casinoId, e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    public ResVisitCasino randomVisit(SimPlayerContext ctx, long lastPlayerId) {
        //切换按钮客户端无冷却, 服务端兜底限频, 避免连点放大随机拜访读链路 (玩家线程串行, 直接用 ctx 字段)
        long now = System.currentTimeMillis();
        if (now - ctx.getLastRandomVisitTime() < RANDOM_VISIT_MIN_INTERVAL_MILLIS) {
            return visitFailure(Code.FORBID);
        }
        ctx.setLastRandomVisitTime(now);
        Set<Long> excludes = new HashSet<>();
        excludes.add(ctx.playerId());
        if (lastPlayerId > 0) {
            excludes.add(lastPlayerId);
        }
        for (SimBaseData candidate : simPlayerGameDao.findRandomVisitCandidates(RANDOM_SAMPLE_SIZE)) {
            long playerId = candidate.getPlayerId();
            if (excludes.contains(playerId) || candidate.getCurrentCasinoId() <= 0) {
                continue;
            }
            ResVisitCasino result = visit(ctx, playerId, candidate.getCurrentCasinoId());
            if (result.code == Code.SUCCESS) {
                log.info("玩家拜访 playerId={},targetPlayerId={}", ctx.playerId(), playerId);
                return result;
            }
            if (result.code == Code.EXCEPTION) {
                //基础设施异常时立即返回，避免对后续候选重复放大 DB/Redis 故障流量。
                return result;
            }
        }
        return visitFailure(Code.NOT_FOUND);
    }

    public ResVisitAction like(SimPlayerContext ctx, long playerId, int casinoId) {
        TargetResult target = findTarget(ctx.playerId(), playerId, casinoId);
        if (!target.success()) {
            return actionFailure(target.code());
        }
        int sourceRemaining = quotaService.consume(SimVisitConstant.QuotaType.LIKE,
                ctx.playerId(), 1, configService.getDailyLikeLimit());
        if (sourceRemaining < 0) {
            return actionFailure(Code.DAILY_LIMIT);
        }
        int points = configService.getLikePopularity();
        int ownerRemaining = quotaService.consume(SimVisitConstant.QuotaType.POPULARITY,
                playerId, points, configService.getDailyPopularityLimit());
        if (ownerRemaining < 0) {
            quotaService.rollback(SimVisitConstant.QuotaType.LIKE, ctx.playerId(), 1);
            return actionFailure(Code.DAILY_LIMIT);
        }
        SimVisitProfileData profile = persistInteraction(ctx, playerId, casinoId,
                SimVisitConstant.RecordType.LIKE, 0, 0, points, 0, null);
        if (profile == null) {
            quotaService.rollback(SimVisitConstant.QuotaType.LIKE, ctx.playerId(), 1);
            quotaService.rollback(SimVisitConstant.QuotaType.POPULARITY, playerId, points);
            return actionFailure(Code.EXCEPTION);
        }
        addRankPopularity(playerId, points);
        return actionSuccess(profile, points, sourceRemaining, ownerRemaining, 0);
    }

    public ResVisitAction comment(SimPlayerContext ctx, long playerId, int casinoId, String content) {
        if (!canComment(ctx.playerId())) {
            return actionFailure(Code.FORBID);
        }
        content = content == null ? null : content.trim();
        if (!SimVisitConfigService.isValidComment(content, configService.getCommentMaxLength())) {
            return actionFailure(Code.PARAM_ERROR);
        }
        TargetResult target = findTarget(ctx.playerId(), playerId, casinoId);
        if (!target.success()) {
            return actionFailure(target.code());
        }
        int sourceRemaining = quotaService.consume(SimVisitConstant.QuotaType.COMMENT,
                ctx.playerId(), 1, configService.getDailyCommentLimit());
        if (sourceRemaining < 0) {
            return actionFailure(Code.COMMONT_DAILY_LIMIT);
        }
        int points = configService.getCommentPopularity();
        int ownerRemaining = quotaService.consume(SimVisitConstant.QuotaType.POPULARITY,
                playerId, points, configService.getDailyPopularityLimit());
        if (ownerRemaining < 0) {
            quotaService.rollback(SimVisitConstant.QuotaType.COMMENT, ctx.playerId(), 1);
            return actionFailure(Code.FORBID);
        }
        SimVisitProfileData profile = persistInteraction(ctx, playerId, casinoId,
                SimVisitConstant.RecordType.COMMENT, 0, 0, points, 0, content);
        if (profile == null) {
            quotaService.rollback(SimVisitConstant.QuotaType.COMMENT, ctx.playerId(), 1);
            quotaService.rollback(SimVisitConstant.QuotaType.POPULARITY, playerId, points);
            return actionFailure(Code.EXCEPTION);
        }
        addRankPopularity(playerId, points);
        return actionSuccess(profile, points, sourceRemaining, ownerRemaining, 0);
    }

    public ResVisitAction gift(SimPlayerContext ctx, long playerId, int casinoId, int giftId) {
        TargetResult target = findTarget(ctx.playerId(), playerId, casinoId);
        if (!target.success()) {
            return actionFailure(target.code());
        }
        GiftListCfg gift = GameDataManager.getGiftListCfg(giftId);
        if (gift == null) {
            return actionFailure(Code.NOT_FOUND);
        }
        Player buyer = sourcePlayer(ctx);
        if (buyer == null) {
            return actionFailure(Code.EXCEPTION);
        }
        int popularity = gift.getPopularity();
        int ownerRemaining = quotaService.consume(SimVisitConstant.QuotaType.POPULARITY,
                playerId, popularity, configService.getDailyPopularityLimit());
        if (ownerRemaining < 0) {
            return actionFailure(Code.DAILY_LIMIT);
        }
        CommonResult<ItemOperationResult> deduct = playerPackService.removeItems(buyer, gift.getCost(),
                AddType.SIM_VISIT_GIFT, "giftId=" + giftId);
        if (!deduct.success()) {
            quotaService.rollback(SimVisitConstant.QuotaType.POPULARITY, playerId, popularity);
            return actionFailure(Code.NOT_ENOUGH);
        }
        SimVisitProfileData profile = persistInteraction(ctx, playerId, casinoId,
                SimVisitConstant.RecordType.GIFT, 0, giftId, popularity, 0, null);
        if (profile == null) {
            quotaService.rollback(SimVisitConstant.QuotaType.POPULARITY, playerId, popularity);
            playerPackService.addItems(ctx.playerId(), gift.getCost(),
                    AddType.SIM_VISIT_GIFT_REFUND, "giftId=" + giftId);
            return actionFailure(Code.EXCEPTION);
        }
        addRankPopularity(playerId, popularity);
        allianceEventService.onVisitGift(ctx.playerId());
        long diamond = deduct.data == null ? 0 : deduct.data.getDiamond();
        return actionSuccess(profile, popularity, 0, ownerRemaining, diamond);
    }

    public ResVisitRecords records(long playerId, int offset, int limit) {
        ResVisitRecords res = new ResVisitRecords(Code.SUCCESS);
        SimVisitProfileData profile = visitDao.findRecordsView(playerId);
        List<SimVisitRecordData> records = profile == null ? List.of() : profile.getRecords();
        res.total = records.size();
        res.records = page(records, offset, limit, configService.getRecordLimit()).stream()
                .map(this::toRecordInfo).toList();
        res.todayPopularity = (int) quotaService.used(SimVisitConstant.QuotaType.POPULARITY, playerId);
        res.totalPopularity = profile == null ? 0 : profile.getTotalPopularity();
        return res;
    }

    /**
     * 留言板属于被拜访的房主: 拜访态下查房主的, 回到自己场景 (visitTargetId=0) 查自己的。
     */
    public ResVisitComments comments(SimPlayerContext ctx, int offset, int limit) {
        long viewerId = ctx.playerId();
        long ownerId = ctx.getVisitTargetId() <= 0 ? viewerId : ctx.getVisitTargetId();
        ResVisitComments res = new ResVisitComments(Code.SUCCESS);
        SimVisitProfileData profile = visitDao.findCommentsView(ownerId);
        List<SimVisitCommentData> comments = profile == null ? List.of() : profile.getComments();
        res.total = comments.size();
        List<SimVisitCommentData> shown = page(comments, offset, limit, configService.getRecordLimit());
        res.comments = shown.stream().map(this::toCommentInfo).toList();
        res.todayPopularity = (int) quotaService.used(SimVisitConstant.QuotaType.POPULARITY, ownerId);
        res.totalPopularity = profile == null ? 0 : profile.getTotalPopularity();
        //只有房主自己翻看才算已读，访客浏览不清房主的未读数
        if (ownerId == viewerId && profile != null && profile.getUnreadCommentCount() > 0
                && !shown.isEmpty()) {
            try {
                visitDao.markCommentsRead(ownerId, shown.stream().map(SimVisitCommentData::getId).toList());
                updateVisitRedDot(ownerId);
            } catch (Exception e) {
                log.error("标记留言已读失败 playerId={}", ownerId, e);
            }
        }
        return res;
    }

    public ResDeleteVisitComment deleteComment(long playerId, String commentId) {
        ResDeleteVisitComment res = new ResDeleteVisitComment(Code.SUCCESS);
        if (commentId == null || commentId.isBlank() || !visitDao.deleteComment(playerId, commentId)) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        res.commentId = commentId;
        updateVisitRedDot(playerId);
        return res;
    }

    public ResVisitSummary summary(long playerId) {
        ResVisitSummary res = new ResVisitSummary(Code.SUCCESS);
        SimVisitProfileData profile = visitDao.findBrief(playerId);
        res.totalPopularity = profile == null ? 0 : profile.getTotalPopularity();
        res.todayPopularity = (int) quotaService.used(SimVisitConstant.QuotaType.POPULARITY, playerId);
        res.commissionGold = quotaService.used(SimVisitConstant.QuotaType.COMMISSION, playerId);
        res.visitorCount = quotaService.visitorCount(playerId);
        return res;
    }

    public ResVisitRank rank(long playerId) {
        return rankService.buildRank(playerId);
    }

    public ResVisitRankRewards rankRewards() {
        return rankService.rankRewards();
    }

    /**
     * 进入客座赌局试玩: 校验"房主已解锁该游戏 + 访客当日试玩次数未达上限", 通过后创建试玩会话
     * (以访客 playerId 为键, 每人同时只能试玩一个房主), 按房主游戏推导单人 slots 场次并切到该节点
     * (范式对齐 SimCoopRoomRouteService.switchToNode / HallRoomService.enterGameNode)。
     * <p>
     * 成功时服务内已回包并切节点, 返回 null; 失败返回带错误码的响应交由 handler 回包。
     */
    public ResVisitTrial enterVisitGame(SimPlayerContext ctx, long ownerId, int casinoId, int gameType) {
        ResVisitTrial res = new ResVisitTrial(Code.SUCCESS);
        TargetResult target = findTarget(ctx.playerId(), ownerId, casinoId);
        if (!target.success()) {
            res.code = target.code();
            return res;
        }
        if (!isGameUnlocked(ownerId, casinoId, gameType)) {
            res.code = Code.NOT_UNLOCKED;
            return res;
        }
        int remaining = quotaService.remaining(SimVisitConstant.QuotaType.TRIAL,
                ctx.playerId(), configService.getDailyTrialLimit());
        if (remaining <= 0) {
            res.code = Code.FORBID;
            return res;
        }
        //先定位单人场次与游戏节点, 拿不到就直接失败, 避免建了会话却切不过去
        Integer roomCfgId = configCacheService.getTrialWareId(gameType);
        MarsNode node = roomCfgId == null ? null : nodeManager.getGameNodeByWeight(
                gameType, ctx.playerId(), ctx.getPlayerController().ipAddress());
        if (node == null) {
            log.warn("进入客座赌局失败,无场次/节点 visitorId={},ownerId={},gameType={},roomCfgId={}",
                    ctx.playerId(), ownerId, gameType, roomCfgId);
            res.code = Code.NOT_FOUND;
            return res;
        }
        long expireTime = System.currentTimeMillis() + configService.getTrialSessionSeconds() * 1000L;
        SimVisitTrialSession session = new SimVisitTrialSession(ctx.playerId(), ownerId,
                casinoId, gameType, expireTime);
        quotaService.saveTrialSession(session, configService.getTrialSessionSeconds());
        res.playerId = ownerId;
        res.casinoId = casinoId;
        res.gameType = gameType;
        res.remainingTrials = remaining;
        res.power = ctx.getSimBaseData() == null ? 0 : ctx.getSimBaseData().getPower();
        res.expireTime = expireTime;
        res.wareId = roomCfgId;
        //先回包再切节点, 客户端切到 slots 节点后按 roomCfgId 进房; enterType=2 让 slots 侧按客座入口加载房主技能
        ctx.send(res);
        playerSessionService.changeGameType(ctx.playerId(), gameType, roomCfgId, EnterGameType.VISIT.getValue(), ownerId + "");
        clusterSystem.switchNode(ctx.getPlayerController().getSession(), node);
        return null;
    }

    public ResVisitTrial exitTrial(long playerId) {
        quotaService.deleteTrialSession(playerId);
        return new ResVisitTrial(Code.SUCCESS);
    }

    /**
     * 被拜访玩家是否已在该场景通过建筑解锁游戏。远端玩家无本地 ctx, 读 Redis 解锁快照。
     */
    private boolean isGameUnlocked(long playerId, int casinoId, int gameType) {
        SimCasinoUnlock casinoUnlock = simCasinoService.getCasinoUnlock(playerId);
        return casinoUnlock != null && casinoUnlock.gameUnlocked(casinoId, gameType);
    }

    /**
     * 仅客座赌局在 slots 出结果前同步调用；普通旋转直接返回 trial=false。
     */
    public CommonResult<VisitTrialSpinPermit> prepareTrialSpin(SimPlayerContext ctx, int gameType,
                                                               boolean freeMode) {
        VisitTrialSpinPermit permit = new VisitTrialSpinPermit();
        SimVisitTrialSession session = quotaService.getTrialSession(ctx.playerId());
        if (session == null) {
            return new CommonResult<>(Code.SUCCESS, permit);
        }
        if (!session.activeFor(ctx.playerId(), gameType, System.currentTimeMillis())) {
            return new CommonResult<>(Code.EXPIRE, permit);
        }
        SimBaseData base = ctx.getSimBaseData();
        if (base == null || (!freeMode && base.getPower() <= 0)) {
            return new CommonResult<>(Code.NOT_ENOUGH, permit);
        }
        int remaining = quotaService.consume(SimVisitConstant.QuotaType.TRIAL,
                ctx.playerId(), 1, configService.getDailyTrialLimit());
        if (remaining < 0) {
            return new CommonResult<>(Code.FORBID, permit);
        }
        if (!freeMode) {
            base.setPower(base.getPower() - SimConstant.Common.SPIN_COST_POWER);
        }
        String permitId = String.valueOf(snowflakeManager.nextId());
        try {
            quotaService.savePendingPermit(ctx.playerId(), permitId,
                    configService.getTrialSessionSeconds());
        } catch (Exception e) {
            if (!freeMode) {
                base.setPower(base.getPower() + SimConstant.Common.SPIN_COST_POWER);
            }
            quotaService.rollback(SimVisitConstant.QuotaType.TRIAL, ctx.playerId(), 1);
            log.error("保存试玩旋转许可失败 playerId={}", ctx.playerId(), e);
            return new CommonResult<>(Code.EXCEPTION, permit);
        }
        permit.setTrial(true);
        permit.setPermitId(permitId);
        permit.setOwnerId(session.getOwnerId());
        permit.setCasinoId(session.getCasinoId());
        permit.setRemainingCount(remaining);
        permit.setPower(base.getPower());
        permit.setFreeMode(freeMode);
        return new CommonResult<>(Code.SUCCESS, permit);
    }

    /**
     * slots 结果生成失败时退回预占。pending key 删除保证重复取消无副作用。
     */
    public boolean cancelTrialSpin(SimPlayerContext ctx, VisitTrialSpinPermit permit) {
        if (permit == null || !permit.isTrial()
                || !quotaService.consumePendingPermit(ctx.playerId(), permit.getPermitId())) {
            return false;
        }
        SimBaseData base = ctx.getSimBaseData();
        if (base != null && !permit.isFreeMode()) {
            base.setPower(base.getPower() + SimConstant.Common.SPIN_COST_POWER);
        }
        quotaService.rollback(SimVisitConstant.QuotaType.TRIAL, ctx.playerId(), 1);
        return true;
    }

    /**
     * 试玩成功结算：人气、记录和房主金币抽成。访客能量已在 prepare 阶段扣除。
     */
    public CommonResult<SlotsSpinResult> settleTrialSpin(SimPlayerContext ctx, int gameType,
                                                         SpinStatInfo statInfo,
                                                         VisitTrialSpinPermit permit) {
        if (permit == null || !permit.isTrial()
                || !quotaService.consumePendingPermit(ctx.playerId(), permit.getPermitId())) {
            return new CommonResult<>(Code.FAIL);
        }
        //pending permit 已由 prepare 阶段在有效会话内创建；即使玩家在结果返回前退出会话，
        //已授权的这一 spin 仍应完成结算，避免白扣能量和次数。

        int popularity = configService.getTrialPopularity();
        int popularityRemaining = quotaService.consume(SimVisitConstant.QuotaType.POPULARITY,
                permit.getOwnerId(), popularity, configService.getDailyPopularityLimit());
        if (popularityRemaining < 0) {
            popularity = 0;
        }

        long commission = 0;
        if (statInfo != null && statInfo.getWin() > 0) {
            long expected = SimVisitConfigService.calculateCommission(statInfo.getWin(),
                    configService.getCommissionRate(), configService.getDailyCommissionLimit());
            commission = quotaService.consumeUpToLong(SimVisitConstant.QuotaType.COMMISSION,
                    permit.getOwnerId(), expected, configService.getDailyCommissionLimit());
        }

        if (commission > 0) {
            //slots 已完成访客输赢结算；房主抽成作为系统侧红利发放，不二次扣减访客金币。
            CommonResult<Player> add = corePlayerService.addGold(permit.getOwnerId(), commission,
                    AddType.SIM_VISIT_COMMISSION,
                    "visitorId=" + ctx.playerId() + ",gameType=" + gameType, true);
            if (!add.success()) {
                quotaService.rollbackLong(SimVisitConstant.QuotaType.COMMISSION,
                        permit.getOwnerId(), commission);
                commission = 0;
            }
        }

        SimVisitProfileData profile = persistInteraction(ctx, permit.getOwnerId(), permit.getCasinoId(),
                SimVisitConstant.RecordType.TRIAL, gameType, 0, popularity, commission, null);
        if (profile == null) {
            if (popularity > 0) {
                quotaService.rollback(SimVisitConstant.QuotaType.POPULARITY,
                        permit.getOwnerId(), popularity);
            }
            if (commission > 0) {
                quotaService.rollbackLong(SimVisitConstant.QuotaType.COMMISSION,
                        permit.getOwnerId(), commission);
                corePlayerService.deductGold(permit.getOwnerId(), commission,
                        AddType.FAIL_ROLLBACK, "sim visit persist rollback", true);
            }
            return new CommonResult<>(Code.EXCEPTION);
        }
        if (popularity > 0) {
            addRankPopularity(permit.getOwnerId(), popularity);
        }
        SlotsSpinResult data = new SlotsSpinResult();
        data.setItemsMap(Map.of());
        data.setPower(ctx.getSimBaseData() == null ? 0 : ctx.getSimBaseData().getPower());
        data.setRemainingTrials(permit.getRemainingCount());

        ItemCfg itemCfg = configCacheService.getResearchPointItemCfg(0);
        if (itemCfg != null) {
            data.setResearchPoints((int) playerPackService.getItemCount(ctx.playerId(), itemCfg.getId()));
        }
        return new CommonResult<>(Code.SUCCESS, data);
    }

    private VisitCasinoInfo buildCasinoInfo(Player player, SimCasinoData casino,
                                            SimVisitProfileData profile, int todayPopularity) {
        VisitCasinoInfo info = new VisitCasinoInfo();
        info.playerId = player.getId();
        info.playerName = player.getNickName();
        info.headImgId = player.getHeadImgId();
        info.headFrameId = player.getHeadFrameId();
        info.casinoId = casino.getCasinoId();
        info.casinoLevel = casino.getCasinoLevel();

        info.roleLevel = simPlayerGameDao.findAllLevelById(player.getId());
        CasinoStatsSheetCfg casinoCfg = configCacheService.getCasinoStatsSheetCfg(
                casino.getCasinoId(), casino.getCasinoLevel());
        info.visitorCapacity = casinoCfg == null ? 0 : casinoCfg.getVisitorSpawnCount();

        info.popularity = profile == null ? 0 : profile.getTotalPopularity();
        info.todayPopularity = todayPopularity;
        info.dailyPopularityLimit = configService.getDailyPopularityLimit();
        info.medalIds = simTaskDao.findDisplayedMedalIds(player.getId());

        Collection<BuildingData> buildings = casino.getBuildingData() == null
                ? List.of() : casino.getBuildingData().values();
        info.buildings = buildings.stream().sorted(Comparator.comparingInt(BuildingData::getId))
                .map(data -> {
                    VisitBuildingInfo pb = new VisitBuildingInfo();
                    pb.id = data.getId();
                    pb.level = data.getLevel();
                    return pb;
                }).toList();

        Set<Integer> unlocked = new HashSet<>();
        for (BuildingData building : buildings) {
            BuildingAreaTableCfg cfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
            if (cfg != null && cfg.getUnlockGameId() > 0) {
                unlocked.add(cfg.getUnlockGameId());
            }
        }
        Map<Integer, SimSkillsData> skills = new HashMap<>();
        for (SimSkillsData data : simSkillsDao.findByPlayerId(player.getId())) {
            skills.put(data.getGameType(), data);
        }
        info.games = unlocked.stream().sorted().map(gameType -> {
            VisitGameInfo pb = new VisitGameInfo();
            pb.gameType = gameType;
            SimSkillsData data = skills.get(gameType);

            if (data != null && data.getSkillsMap() != null) {
                pb.skills = new ArrayList<>();
                for (Map.Entry<Integer, SkillDetailData> en : data.getSkillsMap().entrySet()) {
                    pb.skills.add(new KVInfo(en.getKey(), en.getValue().getLevel()));
                }
            }
            return pb;
        }).toList();
        return info;
    }

    private SimVisitProfileData persistInteraction(SimPlayerContext ctx, long playerId, int casinoId,
                                                   int type, int gameType, int giftId,
                                                   int popularity, long commissionGold,
                                                   String content) {
        try {
            Player visitor = sourcePlayer(ctx);
            if (visitor == null) {
                return null;
            }
            String id = String.valueOf(snowflakeManager.nextId());
            long now = System.currentTimeMillis();
            SimVisitRecordData record = new SimVisitRecordData();
            record.setId(id);
            record.setVisitorId(visitor.getId());
            record.setVisitorName(visitor.getNickName());
            record.setHeadImgId(visitor.getHeadImgId());
            record.setHeadFrameId(visitor.getHeadFrameId());
            record.setCasinoId(casinoId);
            record.setType(type);
            record.setGameType(gameType);
            record.setGiftId(giftId);
            record.setPopularity(popularity);
            record.setCommissionGold(commissionGold);
            record.setCreateTime(now);

            SimVisitCommentData comment = null;
            if (content != null) {
                comment = new SimVisitCommentData();
                comment.setId(id);
                comment.setVisitorId(visitor.getId());
                comment.setVisitorName(visitor.getNickName());
                comment.setHeadImgId(visitor.getHeadImgId());
                comment.setHeadFrameId(visitor.getHeadFrameId());
                comment.setCasinoId(casinoId);
                comment.setContent(content);
                comment.setPopularity(popularity);
                comment.setCreateTime(now);
            }
            SimVisitProfileData profile = visitDao.addInteraction(playerId, popularity, record, comment,
                    configService.getRecordLimit());
            if (profile != null && comment != null) {
                updateVisitRedDot(playerId);
            }
            return profile;
        } catch (Exception e) {
            log.error("保存拜访互动失败 visitorId={},ownerId={},type={}", ctx.playerId(), playerId, type, e);
            return null;
        }
    }

    private void updateVisitRedDot(long playerId) {
        try {
            redDotManager.updateRedDot(initialize(playerId, 0), playerId);
        } catch (Exception e) {
            log.error("更新拜访留言红点失败 playerId={}", playerId, e);
        }
    }

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.VISIT;
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        SimVisitProfileData profile = visitDao.findBrief(playerId);
        int count = profile == null ? 0 : Math.max(0, profile.getUnreadCommentCount());
        return List.of(redDotManager.buildRedDotDetails(getModule(), getSubmodule(), count));
    }

    private TargetResult findTarget(long visitorId, long playerId, int casinoId) {
        if (visitorId == playerId) {
            return new TargetResult(null, null, Code.FORBID);
        }
        Player player = corePlayerService.get(playerId);
        if (player == null) {
            return new TargetResult(null, null, Code.NOT_FOUND);
        }
        SimCasinoData casino = simCasinoDao.findOne(playerId, casinoId);
        if (casino == null) {
            return new TargetResult(null, null, Code.NOT_FOUND);
        }
        return new TargetResult(player, casino, Code.SUCCESS);
    }

    private Player sourcePlayer(SimPlayerContext ctx) {
        if (ctx.getPlayerController() != null && ctx.getPlayerController().getPlayer() != null) {
            return ctx.getPlayerController().getPlayer();
        }
        return corePlayerService.get(ctx.playerId());
    }

    private boolean canComment(long playerId) {
        try {
            if (configService.getCommentRechargeAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return true;
            }
            BigDecimal recharge = countDao.getCount(
                    CountDao.CountType.RECHARGE.getParam(), String.valueOf(playerId));
            return recharge.compareTo(configService.getCommentRechargeAmount()) >= 0;
        } catch (Exception e) {
            log.error("读取拜访留言充值门槛失败 playerId={}", playerId, e);
            return false;
        }
    }

    private ResVisitAction actionSuccess(SimVisitProfileData profile, int points,
                                         int sourceRemaining, int ownerRemaining, long diamond) {
        ResVisitAction res = new ResVisitAction(Code.SUCCESS);
        res.addedPopularity = points;
        res.totalPopularity = profile.getTotalPopularity();
        res.todayPopularity = configService.getDailyPopularityLimit() - ownerRemaining;
        res.remainingCount = sourceRemaining;
        res.diamond = diamond;
        return res;
    }

    private void addRankPopularity(long playerId, int points) {
        try {
            rankService.addPopularity(playerId, points);
        } catch (Exception e) {
            //持久化人气已经成功，排行榜故障不能让客户端重复提交同一互动。
            log.error("更新拜访人气榜失败 playerId={},points={}", playerId, points, e);
        }
    }

    private ResVisitAction actionFailure(int code) {
        return new ResVisitAction(code);
    }

    private ResVisitCasino visitFailure(int code) {
        return new ResVisitCasino(code);
    }

    static <T> List<T> page(List<T> source, int offset, int limit, int maxLimit) {
        if (source == null || source.isEmpty() || offset < 0 || offset >= source.size()) {
            return List.of();
        }
        int size = Math.max(1, Math.min(limit, maxLimit));
        int end = Math.min(source.size(), offset + size);
        return new ArrayList<>(source.subList(offset, end));
    }

    private VisitRecordInfo toRecordInfo(SimVisitRecordData data) {
        VisitRecordInfo info = new VisitRecordInfo();
        info.id = data.getId();
        info.visitorId = data.getVisitorId();
        info.visitorName = data.getVisitorName();
        info.headImgId = data.getHeadImgId();
        info.headFrameId = data.getHeadFrameId();
        info.casinoId = data.getCasinoId();
        info.type = data.getType();
        info.gameType = data.getGameType();
        info.giftId = data.getGiftId();
        info.popularity = data.getPopularity();
        info.commissionGold = data.getCommissionGold();
        info.createTime = data.getCreateTime();
        return info;
    }

    private VisitCommentInfo toCommentInfo(SimVisitCommentData data) {
        VisitCommentInfo info = new VisitCommentInfo();
        info.id = data.getId();
        info.visitorId = data.getVisitorId();
        info.visitorName = data.getVisitorName();
        info.headImgId = data.getHeadImgId();
        info.headFrameId = data.getHeadFrameId();
        info.casinoId = data.getCasinoId();
        info.content = data.getContent();
        info.popularity = data.getPopularity();
        info.createTime = data.getCreateTime();
        return info;
    }

    private record TargetResult(Player player, SimCasinoData casino, int code) {
        boolean success() {
            return code == Code.SUCCESS;
        }
    }
}
