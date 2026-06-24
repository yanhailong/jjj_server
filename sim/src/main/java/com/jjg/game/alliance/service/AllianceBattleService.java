package com.jjg.game.alliance.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AllianceBattleDao;
import com.jjg.game.alliance.dao.AllianceDao;
import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceBattleData;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.data.BattleResult;
import com.jjg.game.alliance.data.BattleSignup;
import com.jjg.game.alliance.pb.AlliancePbConverter;
import com.jjg.game.alliance.pb.res.ResAllianceBattleInfo;
import com.jjg.game.alliance.pb.res.ResAllianceBattleRank;
import com.jjg.game.alliance.pb.res.ResAllianceBattleSignup;
import com.jjg.game.alliance.pb.res.ResBattleStageClaim;
import com.jjg.game.alliance.pb.struct.BattleStageInfo;
import com.jjg.game.alliance.pb.struct.ContribRankInfo;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.RankEntry;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.service.RankService;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 联盟对决 (周期性联盟 PVP): 报名 / 匹配 / 积分 / 阶段奖励 / 结算。
 * <p>
 * 架构要点:
 * <ul>
 *   <li>阶段状态机由集群 leader 定时 {@link #tick} 推进, 每次迁移用"旧状态条件更新"幂等化,
 *       leader 切换/并发 tick 双跑无副作用; 宕机错过窗口时按时间连续补推;</li>
 *   <li><b>镜像协议</b>: 匹配结果不要求对称(A 的对手是 B 时 B 的对手可以是 C), 每盟只对自己对局
 *       的胜负负责, 任意报名数都能匹配; 唯一报名的联盟轮空(对手=0, 结算按胜利);</li>
 *   <li>积分是最高频写路径(每次消耗体力都可能掉落), 全部走 Redis zset 累加, Mongo 只存
 *       报名/匹配/结算快照; 本期文档走 10s 本地缓存, 无对决/未开战时在缓存上短路;</li>
 *   <li>积分归属: 掉落瞬间按玩家当前所在联盟入账 —— 自动满足"退盟不带走已交积分,
 *       新积分给新盟"; 联盟解散积分冻结, 结算时联盟不存在则不发奖(对手照常)。</li>
 * </ul>
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceBattleService {
    private static final Logger log = LoggerFactory.getLogger(AllianceBattleService.class);

    @Autowired
    private AllianceBattleDao battleDao;
    @Autowired
    private AllianceDao allianceDao;
    @Autowired
    private AlliancePlayerDao alliancePlayerDao;
    @Autowired
    private AllianceCacheService cacheService;
    @Autowired
    private AllianceConfigService configService;
    @Autowired
    private RankService rankService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private MailService mailService;

    //本期文档本地缓存: 高频掉落路径的短路屏障 (10s 滞后只影响开战边界几秒的积分, 可接受)
    private final Cache<String, Optional<AllianceBattleData>> battleCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(4)
            .build();

    // =====================================================================
    // 阶段推进 (仅 leader 调度调用, 见 AllianceManager)
    // =====================================================================

    /**
     * 推进本期状态机: 创建期文档 + 按时间逐步迁移状态。每步条件更新, 任意并发/重入安全。
     */
    public void tick() {
        long now = System.currentTimeMillis();
        String period = configService.battlePeriod(now);
        AllianceBattleData battle = battleDao.findById(period).orElse(null);
        if (battle == null) {
            //创建本期 (时间点固化, 改配置不影响进行中的期)
            battle = new AllianceBattleData();
            battle.setPeriod(period);
            battle.setState(AllianceConst.BattleState.NONE);
            battle.setSignupStartTime(configService.battleSignupStart(now));
            battle.setSignupEndTime(configService.battleSignupEnd(now));
            battle.setMatchTime(configService.battleMatchTime(now));
            battle.setFightStartTime(configService.battleFightStart(now));
            battle.setFightEndTime(configService.battleFightEnd(now));
            battleDao.insertIfAbsent(battle);
            battle = battleDao.findById(period).orElse(null);
            if (battle == null) {
                return;
            }
        }
        //逐步推进 (宕机错过窗口时一次 tick 连续补推到位)
        for (int i = 0; i < 5; i++) {
            int state = battle.getState();
            boolean advanced = switch (state) {
                case AllianceConst.BattleState.NONE -> now >= battle.getSignupStartTime()
                        && battleDao.tryAdvanceState(period, state, AllianceConst.BattleState.SIGNUP);
                case AllianceConst.BattleState.SIGNUP -> now >= battle.getSignupEndTime()
                        && battleDao.tryAdvanceState(period, state, AllianceConst.BattleState.SIGNUP_CLOSED);
                case AllianceConst.BattleState.SIGNUP_CLOSED -> now >= battle.getMatchTime()
                        && doMatch(battle);
                case AllianceConst.BattleState.MATCHED -> now >= battle.getFightStartTime()
                        && battleDao.tryAdvanceState(period, state, AllianceConst.BattleState.FIGHTING);
                case AllianceConst.BattleState.FIGHTING -> now >= battle.getFightEndTime()
                        && doSettle(battle);
                default -> false;
            };
            if (!advanced) {
                break;
            }
            battleCache.invalidate(period);
            battle = battleDao.findById(period).orElse(null);
            if (battle == null) {
                return;
            }
        }
    }

    /**
     * 匹配: 报名联盟按等级分组随机两两配对; 组内落单者跨组配对; 最终落单/唯一者按镜像协议处理。
     * 与状态迁移同一条件更新原子提交。
     */
    private boolean doMatch(AllianceBattleData battle) {
        List<Long> signed = new ArrayList<>(battle.getSignups().keySet());
        Map<Long, Long> matches = new HashMap<>();
        if (!signed.isEmpty()) {
            //按报名时联盟等级分组
            Map<Integer, List<Long>> byLevel = new HashMap<>();
            for (Long aid : signed) {
                BattleSignup signup = battle.getSignups().get(aid);
                byLevel.computeIfAbsent(signup == null ? 1 : signup.getLevel(), k -> new ArrayList<>()).add(aid);
            }
            List<Long> leftovers = new ArrayList<>();
            for (List<Long> group : byLevel.values()) {
                Collections.shuffle(group);
                int i = 0;
                for (; i + 1 < group.size(); i += 2) {
                    matches.put(group.get(i), group.get(i + 1));
                    matches.put(group.get(i + 1), group.get(i));
                }
                if (i < group.size()) {
                    leftovers.add(group.get(i));
                }
            }
            //落单者跨等级配对
            Collections.shuffle(leftovers);
            int i = 0;
            for (; i + 1 < leftovers.size(); i += 2) {
                matches.put(leftovers.get(i), leftovers.get(i + 1));
                matches.put(leftovers.get(i + 1), leftovers.get(i));
            }
            //最终落单: 镜像协议 —— 随机对阵任一其他报名联盟的镜像 (战绩独立, 互不影响)
            if (i < leftovers.size()) {
                long lone = leftovers.get(i);
                List<Long> others = signed.stream().filter(a -> a != lone).toList();
                //唯一报名: 轮空 (对手=0, 结算按胜利)
                long opp = others.isEmpty() ? 0 : others.get(RandomUtils.randomInt(others.size()));
                matches.put(lone, opp);
            }
        }
        boolean ok = battleDao.saveMatchesAndAdvance(battle.getPeriod(),
                AllianceConst.BattleState.SIGNUP_CLOSED, AllianceConst.BattleState.MATCHED, matches);
        if (ok) {
            log.info("联盟对决匹配完成 period={},signed={},matches={}", battle.getPeriod(), signed.size(), matches.size());
        }
        return ok;
    }

    // =====================================================================
    // 报名
    // =====================================================================

    /**
     * 报名 (盟主)。条件: 报名窗口内 / 联盟等级 / 人数(或活跃人数)达标。
     */
    public ResAllianceBattleSignup signup(long playerId) {
        var res = new ResAllianceBattleSignup(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("联盟对决报名失败,玩家不在联盟 playerId={}", playerId);
            return res;
        }
        if (!alliance.isLeader(playerId)) {
            res.code = Code.FORBID;
            log.warn("联盟对决报名失败,非盟主 playerId={},allianceId={},leaderId={}", playerId, allianceId, alliance.getLeaderId());
            return res;
        }
        if (alliance.getLevel() < AllianceConst.Cfg.BATTLE_MIN_LEVEL) {
            res.code = Code.LEVEL_NOT_ENOUGH;
            log.warn("联盟对决报名失败,联盟等级不足 allianceId={},level={},minLevel={}", allianceId, alliance.getLevel(), AllianceConst.Cfg.BATTLE_MIN_LEVEL);
            return res;
        }
        //人数门槛: 模式1=总人数 / 模式2=近N天活跃人数 (登录时维护的 lastActiveTime, 单文档统计)
        int count = AllianceConst.Cfg.BATTLE_SIGNUP_MODE == 1
                ? alliance.getMemberCount()
                : alliance.activeMemberCount(AllianceConst.Cfg.BATTLE_ACTIVE_DAYS);
        if (count < AllianceConst.Cfg.BATTLE_MIN_MEMBERS) {
            res.code = Code.NOT_ENOUGH;
            log.warn("联盟对决报名失败,人数不足 allianceId={},mode={},count={},minMembers={}", allianceId, AllianceConst.Cfg.BATTLE_SIGNUP_MODE, count, AllianceConst.Cfg.BATTLE_MIN_MEMBERS);
            return res;
        }
        long now = System.currentTimeMillis();
        String period = configService.battlePeriod(now);
        AllianceBattleData battle = battleDao.findById(period).orElse(null);
        if (battle == null || battle.getState() != AllianceConst.BattleState.SIGNUP) {
            res.code = Code.FORBID;
            log.warn("联盟对决报名失败,非报名阶段 allianceId={},period={},state={}", allianceId, period, battle == null ? -1 : battle.getState());
            return res;
        }
        if (!battleDao.addSignup(period, allianceId,
                new BattleSignup(alliance.getLevel(), now, playerId), AllianceConst.BattleState.SIGNUP)) {
            res.code = Code.REPEAT_OP;
            log.warn("联盟对决报名失败,重复报名或阶段已变更 allianceId={},period={}", allianceId, period);
            return res;
        }
        battleCache.invalidate(period);
        log.info("联盟对决报名 period={},allianceId={},leader={}", period, allianceId, playerId);
        return res;
    }

    // =====================================================================
    // 积分 (高频路径)
    // =====================================================================

    /**
     * 消耗体力掉落对决积分 (由 AllianceEventService 调用)。
     * 短路顺序: 期缓存(非战斗中) -> 玩家无盟 -> 未匹配 -> 概率, 绝大多数调用零存储访问。
     */
    public void onPowerConsumed(long playerId, int costPower) {
        AllianceBattleData battle = cachedBattle();
        long now = System.currentTimeMillis();
        if (battle == null || !battle.fighting(now)) {
            return;
        }
        long allianceId = cacheService.getAllianceId(playerId);
        if (allianceId <= 0 || !battle.getMatches().containsKey(allianceId)) {
            return;
        }
        //概率掉落 (万分比)
        if (!RandomUtils.getRandomBoolean10000(AllianceConst.Cfg.BATTLE_DROP_PROB)) {
            return;
        }
        int score = AllianceConst.Cfg.BATTLE_DROP_SCORE;
        rankService.addPoints(scoreKey(battle.getPeriod()), allianceId, score);
        rankService.addPoints(personalKey(battle.getPeriod(), allianceId), playerId, score);
    }

    // =====================================================================
    // 展示
    // =====================================================================

    /**
     * 对决信息: 阶段/时间点/报名状态/对手/比分/个人分/阶段奖励。
     */
    public ResAllianceBattleInfo battleInfo(long playerId) {
        ResAllianceBattleInfo res = new ResAllianceBattleInfo(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceBattleData battle = cachedBattle();
        if (battle == null) {
            res.state = AllianceConst.BattleState.NONE;
            return res;
        }
        res.period = battle.getPeriod();
        res.state = battle.getState();
        res.signupStartTime = battle.getSignupStartTime();
        res.signupEndTime = battle.getSignupEndTime();
        res.fightStartTime = battle.getFightStartTime();
        res.fightEndTime = battle.getFightEndTime();
        if (allianceId <= 0) {
            return res;
        }
        res.signedUp = battle.signedUp(allianceId);
        long opp = battle.opponentOf(allianceId);
        if (opp > 0) {
            res.opponent = AlliancePbConverter.toBrief(cacheService.getAlliance(opp), configService);
        }
        if (battle.getMatches().containsKey(allianceId)) {
            res.myScore = rankService.getPoints(scoreKey(battle.getPeriod()), allianceId);
            res.oppScore = opp > 0 ? rankService.getPoints(scoreKey(battle.getPeriod()), opp) : 0;
            res.myPersonalScore = rankService.getPoints(personalKey(battle.getPeriod(), allianceId), playerId);
        }
        //已结算: 用快照结果
        BattleResult result = battle.getResults().get(allianceId);
        if (result != null) {
            res.myScore = result.getMyScore();
            res.oppScore = result.getOppScore();
            res.win = result.isWin();
        }
        //阶段奖励
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int claimedMask = playerData.battleClaimedMask(battle.getPeriod());
        res.stages = new ArrayList<>();
        for (AllianceConfigService.BattleStageCfg stage : configService.battleStages()) {
            BattleStageInfo info = new BattleStageInfo();
            info.stage = stage.stage();
            info.scoreThreshold = stage.scoreThreshold();
            info.rewards = AlliancePbConverter.toItemInfos(stage.rewards());
            info.claimed = (claimedMask & (1 << stage.stage())) != 0;
            res.stages.add(info);
        }
        return res;
    }

    /**
     * 对决贡献榜单 (盟内个人比赛值, 显示全部)。
     */
    public ResAllianceBattleRank battleRank(long playerId) {
        ResAllianceBattleRank res = new ResAllianceBattleRank(Code.SUCCESS);
        res.list = new ArrayList<>();
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceBattleData battle = cachedBattle();
        if (allianceId <= 0 || battle == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        String key = personalKey(battle.getPeriod(), allianceId);
        List<RankEntry> entries = rankService.topN(key, AllianceConst.Cfg.BATTLE_RANK_SHOW);
        List<Long> pids = entries.stream().map(RankEntry::getPlayerId).toList();
        Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(pids);
        int rank = 1;
        for (RankEntry entry : entries) {
            ContribRankInfo info = toRankInfo(entry.getPlayerId(), rank++, entry.getPoints(), playerMap);
            if (info != null) {
                res.list.add(info);
            }
        }
        RankEntry myEntry = rankService.getRank(key, playerId);
        res.my = toRankInfo(playerId, myEntry == null ? -1 : (int) myEntry.getRank(),
                myEntry == null ? 0 : myEntry.getPoints(), corePlayerService.multiGetPlayerMap(List.of(playerId)));
        return res;
    }

    /**
     * 领取阶段奖励 (个人): 个人积分达标且未领取。
     */
    public ResBattleStageClaim claimStage(long playerId, int stage) {
        ResBattleStageClaim res = new ResBattleStageClaim(Code.SUCCESS);
        res.stage = stage;
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceBattleData battle = cachedBattle();
        if (allianceId <= 0 || battle == null) {
            res.code = Code.NOT_FOUND;
            log.warn("领取对决阶段奖励失败,玩家不在联盟或无进行中对决 playerId={},allianceId={},stage={}", playerId, allianceId, stage);
            return res;
        }
        AllianceConfigService.BattleStageCfg cfg = configService.battleStages().stream()
                .filter(s -> s.stage() == stage).findFirst().orElse(null);
        if (cfg == null) {
            res.code = Code.PARAM_ERROR;
            log.warn("领取对决阶段奖励失败,阶段配置不存在 playerId={},stage={}", playerId, stage);
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int claimedMask = playerData.battleClaimedMask(battle.getPeriod());
        if ((claimedMask & (1 << stage)) != 0) {
            res.code = Code.REPEAT_OP;
            log.warn("领取对决阶段奖励失败,已领取过 playerId={},period={},stage={}", playerId, battle.getPeriod(), stage);
            return res;
        }
        long personalScore = rankService.getPoints(personalKey(battle.getPeriod(), allianceId), playerId);
        if (personalScore < cfg.scoreThreshold()) {
            res.code = Code.NOT_ENOUGH;
            log.warn("领取对决阶段奖励失败,个人积分不足 playerId={},period={},stage={},score={},threshold={}", playerId, battle.getPeriod(), stage, personalScore, cfg.scoreThreshold());
            return res;
        }
        int stageMask = 1 << stage;
        if (!alliancePlayerDao.tryClaimBattleStage(playerId, battle.getPeriod(), stageMask)) {
            res.code = Code.REPEAT_OP;
            log.warn("领取对决阶段奖励失败,并发重复领取 playerId={},period={},stage={}", playerId, battle.getPeriod(), stage);
            return res;
        }
        playerPackService.addItems(playerId, cfg.rewards(), AddType.ALLIANCE_BATTLE_REWARD, "联盟对决阶段奖励", true);
        res.rewards = AlliancePbConverter.toItemInfos(cfg.rewards());
        log.info("领取对决阶段奖励 playerId={},period={},stage={}", playerId, battle.getPeriod(), stage);
        return res;
    }

    // =====================================================================
    // 结算
    // =====================================================================

    /**
     * 结算: 冻结快照比分 -> 条件落库(幂等屏障) -> 发奖。
     * 镜像结算规则(需求): 只看自己对局的输赢, 不给对手发奖; 平局按胜利发; 轮空(对手=0)按胜利发。
     */
    private boolean doSettle(AllianceBattleData battle) {
        String period = battle.getPeriod();
        //1. 冻结快照
        Map<Long, BattleResult> results = new HashMap<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, Long> en : battle.getMatches().entrySet()) {
            long aid = en.getKey();
            long opp = en.getValue() == null ? 0 : en.getValue();
            long myScore = rankService.getPoints(scoreKey(period), aid);
            long oppScore = opp > 0 ? rankService.getPoints(scoreKey(period), opp) : 0;
            results.put(aid, new BattleResult(opp, myScore, oppScore, myScore >= oppScore, now));
        }
        //2. 条件落库: FIGHTING -> SETTLED 只有一个节点能成功, 发奖只执行一次
        if (!battleDao.saveResultsAndAdvance(period, AllianceConst.BattleState.FIGHTING,
                AllianceConst.BattleState.SETTLED, results)) {
            return false;
        }
        //3. 发奖
        for (Map.Entry<Long, BattleResult> en : results.entrySet()) {
            try {
                settleAlliance(period, en.getKey(), en.getValue());
            } catch (Exception e) {
                log.error("对决联盟结算发奖失败 period={},allianceId={}", period, en.getKey(), e);
            }
        }
        log.info("联盟对决结算完成 period={},alliances={}", period, results.size());
        return true;
    }

    /**
     * 单联盟发奖: 全员胜负奖励邮件 + 未领取的阶段奖励邮件补发。
     * 已解散的联盟跳过 (积分冻结不发奖, 需求); 未领奖但已退盟的玩家不补发阶段奖励 (需求)。
     */
    private void settleAlliance(String period, long allianceId, BattleResult result) {
        AllianceData alliance = allianceDao.findById(allianceId).orElse(null);
        if (alliance == null) {
            return;
        }
        Map<Integer, Long> rewards = result.isWin()
                ? configService.battleWinRewards() : configService.battleLoseRewards();
        String title = result.isWin() ? "联盟对决胜利奖励" : "联盟对决参与奖励";
        String content = "联盟对决已结束, 比分 " + result.getMyScore() + " : " + result.getOppScore()
                + (result.isWin() ? ", 恭喜获胜!" : ", 虽败犹荣, 再接再厉!");
        List<Item> mailItems = toMailItems(rewards);
        for (Long pid : alliance.getMembers().keySet()) {
            mailService.addMail(pid, title, content, mailItems, AddType.ALLIANCE_BATTLE_REWARD);
        }
        //阶段奖励补发: 个人分达标但未手动领取的现任成员
        List<RankEntry> entries = rankService.topN(personalKey(period, allianceId), AllianceConst.Cfg.BATTLE_RANK_SHOW);
        for (RankEntry entry : entries) {
            long pid = entry.getPlayerId();
            if (!alliance.isMember(pid)) {
                //退盟玩家不补发 (需求明确)
                continue;
            }
            AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(pid);
            int claimedMask = playerData.battleClaimedMask(period);
            int newMask = claimedMask;
            for (AllianceConfigService.BattleStageCfg stage : configService.battleStages()) {
                if (entry.getPoints() < stage.scoreThreshold() || (claimedMask & (1 << stage.stage())) != 0) {
                    continue;
                }
                mailService.addMail(pid, "联盟对决阶段奖励",
                        "对决已结束, 未领取的第" + (stage.stage() + 1) + "阶段奖励已通过邮件发放。",
                        toMailItems(stage.rewards()), AddType.ALLIANCE_BATTLE_REWARD);
                newMask |= (1 << stage.stage());
            }
            if (newMask != claimedMask) {
                alliancePlayerDao.setBattleClaim(pid, period, newMask);
            }
        }
        //积分 key 留存 7 天供战绩查询, 之后自动过期
        try {
            redissonClient.getKeys().expire(scoreKey(period), 7, TimeUnit.DAYS);
            redissonClient.getKeys().expire(personalKey(period, allianceId), 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("设置对决积分过期失败 period={}", period, e);
        }
    }

    // =====================================================================
    // 工具
    // =====================================================================

    /**
     * 本期文档 (10s 本地缓存)。
     */
    private AllianceBattleData cachedBattle() {
        String period = configService.battlePeriod(System.currentTimeMillis());
        Optional<AllianceBattleData> opt = battleCache.get(period,
                p -> Optional.ofNullable(battleDao.findById(p).orElse(null)));
        return opt == null ? null : opt.orElse(null);
    }

    private ContribRankInfo toRankInfo(long playerId, int rank, long score, Map<Long, Player> playerMap) {
        Player player = playerMap.get(playerId);
        if (player == null) {
            return null;
        }
        ContribRankInfo info = new ContribRankInfo();
        info.rank = rank;
        info.playerId = playerId;
        info.nick = player.getNickName();
        info.headImg = player.getHeadImgId();
        info.headFrame = player.getHeadFrameId();
        info.level = player.getLevel();
        info.score = score;
        return info;
    }

    private List<Item> toMailItems(Map<Integer, Long> rewards) {
        List<Item> items = new ArrayList<>(rewards.size());
        for (Map.Entry<Integer, Long> en : rewards.entrySet()) {
            items.add(new Item(en.getKey(), en.getValue()));
        }
        return items;
    }

    private String scoreKey(String period) {
        return AllianceConst.RedisKey.BATTLE_SCORE_PREFIX + period;
    }

    private String personalKey(String period, long allianceId) {
        return AllianceConst.RedisKey.BATTLE_PERSONAL_PREFIX + period + ":" + allianceId;
    }
}
