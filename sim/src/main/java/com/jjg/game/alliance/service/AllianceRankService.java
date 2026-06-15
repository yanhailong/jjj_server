package com.jjg.game.alliance.service;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AllianceDao;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.pb.AlliancePbConverter;
import com.jjg.game.alliance.pb.res.ResAllianceRank;
import com.jjg.game.alliance.pb.res.ResContribRank;
import com.jjg.game.alliance.pb.struct.AllianceRankInfo;
import com.jjg.game.alliance.pb.struct.ContribRankInfo;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.RankEntry;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.RankService;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联盟排行榜: 查询展示 + 周/赛季结算。
 * <p>
 * 全部榜单基于 core {@code RankService}(Redis zset, 同分早达先排):
 * <ul>
 *   <li>声誉总榜 — 实时, 声誉入账时累加 (见 AllianceAssetService);</li>
 *   <li>赛季榜(自然月) — 同步累加, 月初由 leader 结算上月并发奖;</li>
 *   <li>贡献度周榜(盟内) — 任务/捐献产生贡献度时累加, 周一由 leader 逐盟结算上周。</li>
 * </ul>
 * 结算入口仅由集群 leader 的调度线程调用 (见 AllianceManager), 且按"榜单 key 是否仍存在"幂等
 * —— 结算完成即删 key, leader 切换重复进入也不会二次发奖。
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceRankService {
    private static final Logger log = LoggerFactory.getLogger(AllianceRankService.class);

    //结算遍历联盟的分页大小
    private static final int SETTLE_PAGE_SIZE = 200;

    @Autowired
    private RankService rankService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private AllianceDao allianceDao;
    @Autowired
    private AllianceCacheService cacheService;
    @Autowired
    private AllianceConfigService configService;
    @Autowired
    private AllianceAssetService assetService;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private MailService mailService;

    // =====================================================================
    // 查询
    // =====================================================================

    /**
     * 贡献度周榜 (盟内): 前 50 名 + 我的排名 (未上榜也展示自己, 需求)。
     */
    public ResContribRank contribRank(long playerId) {
        ResContribRank res = new ResContribRank(Code.SUCCESS);
        res.list = new ArrayList<>();
        long allianceId = cacheService.getAllianceId(playerId);
        if (allianceId <= 0) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        String key = assetService.contribRankKey(allianceId);
        List<RankEntry> entries = rankService.topN(key, AllianceConst.Cfg.CONTRIB_RANK_SHOW);
        List<Long> pids = entries.stream().map(RankEntry::getPlayerId).toList();
        Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(pids);
        int rank = 1;
        for (RankEntry entry : entries) {
            ContribRankInfo info = toContribInfo(entry.getPlayerId(), rank++, entry.getPoints(), playerMap);
            if (info != null) {
                res.list.add(info);
            }
        }
        //我的排名
        RankEntry myEntry = rankService.getRank(key, playerId);
        Map<Long, Player> selfMap = corePlayerService.multiGetPlayerMap(List.of(playerId));
        res.my = toContribInfo(playerId, myEntry == null ? -1 : (int) myEntry.getRank(),
                myEntry == null ? 0 : myEntry.getPoints(), selfMap);
        return res;
    }

    /**
     * 联盟声誉排行 (全服): 前 10 名 + 我盟排名。
     */
    public ResAllianceRank allianceRank(long playerId) {
        ResAllianceRank res = new ResAllianceRank(Code.SUCCESS);
        res.list = new ArrayList<>();
        List<RankEntry> entries = rankService.topN(AllianceConst.RedisKey.RANK_REPUTATION,
                AllianceConst.Cfg.ALLIANCE_RANK_SHOW);
        List<Long> aids = entries.stream().map(RankEntry::getPlayerId).toList();
        Map<Long, AllianceData> briefMap = new HashMap<>();
        for (AllianceData data : allianceDao.multiGetBrief(aids)) {
            briefMap.put(data.getAllianceId(), data);
        }
        int rank = 1;
        for (RankEntry entry : entries) {
            AllianceData data = briefMap.get(entry.getPlayerId());
            if (data == null) {
                continue;
            }
            AllianceRankInfo info = new AllianceRankInfo();
            info.rank = rank++;
            info.alliance = AlliancePbConverter.toBrief(data, configService);
            info.score = entry.getPoints();
            res.list.add(info);
        }
        //我所在联盟的排名 (无盟为 null)
        long myAllianceId = cacheService.getAllianceId(playerId);
        if (myAllianceId > 0) {
            RankEntry myEntry = rankService.getRank(AllianceConst.RedisKey.RANK_REPUTATION, myAllianceId);
            AllianceData myAlliance = cacheService.getAlliance(myAllianceId);
            if (myAlliance != null) {
                AllianceRankInfo my = new AllianceRankInfo();
                my.rank = myEntry == null ? -1 : (int) myEntry.getRank();
                my.alliance = AlliancePbConverter.toBrief(myAlliance, configService);
                my.score = myEntry == null ? myAlliance.getReputation() : myEntry.getPoints();
                res.my = my;
            }
        }
        return res;
    }

    private ContribRankInfo toContribInfo(long playerId, int rank, long score, Map<Long, Player> playerMap) {
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

    // =====================================================================
    // 移除 (退盟/解散)
    // =====================================================================

    /**
     * 从盟内贡献周榜移除玩家 (退盟/被踢)。
     */
    public void removeFromContribRank(long allianceId, long playerId) {
        try {
            redissonClient.getScoredSortedSet(assetService.contribRankKey(allianceId), LongCodec.INSTANCE)
                    .remove(playerId);
        } catch (Exception e) {
            log.warn("移除贡献周榜失败 allianceId={},playerId={}", allianceId, playerId, e);
        }
    }

    /**
     * 联盟解散: 从声誉总榜/赛季榜移除, 删除盟内周榜。
     */
    public void removeAllianceFromRanks(long allianceId) {
        try {
            redissonClient.getScoredSortedSet(AllianceConst.RedisKey.RANK_REPUTATION, LongCodec.INSTANCE)
                    .remove(allianceId);
            redissonClient.getScoredSortedSet(assetService.seasonRankKey(), LongCodec.INSTANCE)
                    .remove(allianceId);
            redissonClient.getKeys().delete(assetService.contribRankKey(allianceId));
        } catch (Exception e) {
            log.warn("移除联盟榜单失败 allianceId={}", allianceId, e);
        }
    }

    // =====================================================================
    // 结算 (仅 leader 调度调用)
    // =====================================================================

    /**
     * 结算上周贡献度周榜: 遍历全部联盟, 给上榜成员发奖励邮件, 完成即删榜单 key (幂等依据)。
     */
    public void settleWeeklyContrib() {
        String lastWeek = lastWeekKey();
        long lastId = 0;
        int settled = 0;
        while (true) {
            List<Long> page = allianceDao.pageIds(lastId, SETTLE_PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            for (Long aid : page) {
                String key = assetService.contribRankKey(aid, lastWeek);
                try {
                    //key 不存在 = 无数据或已结算 (幂等)
                    if (redissonClient.getKeys().countExists(key) <= 0) {
                        continue;
                    }
                    List<RankEntry> entries = rankService.topN(key, AllianceConst.Cfg.CONTRIB_RANK_SHOW);
                    for (RankEntry entry : entries) {
                        Map<Integer, Long> rewards = configService.contribRankRewards((int) entry.getRank());
                        if (rewards.isEmpty()) {
                            continue;
                        }
                        mailService.addMail(entry.getPlayerId(), "联盟贡献度周榜奖励",
                                "上周贡献度排名第" + entry.getRank() + "名, 奖励已发放, 请查收。",
                                toMailItems(rewards), AddType.ALLIANCE_RANK_REWARD);
                    }
                    redissonClient.getKeys().delete(key);
                    settled++;
                } catch (Exception e) {
                    log.error("贡献周榜结算失败 allianceId={},week={}", aid, lastWeek, e);
                }
            }
            lastId = page.get(page.size() - 1);
        }
        log.info("贡献度周榜结算完成 week={},alliances={}", lastWeek, settled);
    }

    /**
     * 结算上月赛季榜: 给前 N 名联盟的全体成员发奖励邮件, 完成即删榜单 key (幂等依据)。
     */
    public void settleSeason() {
        int lastMonth = lastMonthKey();
        String key = assetService.seasonRankKey(lastMonth);
        try {
            //key 不存在 = 无数据或已结算 (幂等)
            if (redissonClient.getKeys().countExists(key) <= 0) {
                return;
            }
            List<RankEntry> entries = rankService.topN(key, AllianceConst.Cfg.ALLIANCE_RANK_SHOW);
            for (RankEntry entry : entries) {
                long aid = entry.getPlayerId();
                Map<Integer, Long> rewards = configService.seasonRankRewards((int) entry.getRank());
                if (rewards.isEmpty()) {
                    continue;
                }
                AllianceData alliance = allianceDao.findById(aid).orElse(null);
                if (alliance == null) {
                    //赛季中解散的联盟不发奖
                    continue;
                }
                for (Long pid : alliance.getMembers().keySet()) {
                    mailService.addMail(pid, "联盟赛季排行奖励",
                            "联盟[" + alliance.getName() + "]上赛季排名第" + entry.getRank() + "名, 奖励已发放, 请查收。",
                            toMailItems(rewards), AddType.ALLIANCE_RANK_REWARD);
                }
            }
            redissonClient.getKeys().delete(key);
            log.info("联盟赛季榜结算完成 month={},rewarded={}", lastMonth, entries.size());
        } catch (Exception e) {
            log.error("联盟赛季榜结算失败 month={}", lastMonth, e);
        }
    }

    // =====================================================================
    // 工具
    // =====================================================================

    private List<Item> toMailItems(Map<Integer, Long> rewards) {
        List<Item> items = new ArrayList<>(rewards.size());
        for (Map.Entry<Integer, Long> en : rewards.entrySet()) {
            items.add(new Item(en.getKey(), en.getValue()));
        }
        return items;
    }

    /**
     * 上一 ISO 周标识
     */
    private String lastWeekKey() {
        LocalDate date = LocalDate.now().minusWeeks(1);
        WeekFields wf = WeekFields.ISO;
        return date.get(wf.weekBasedYear()) + "W" + String.format("%02d", date.get(wf.weekOfWeekBasedYear()));
    }

    /**
     * 上一自然月 (yyyyMM)
     */
    private int lastMonthKey() {
        LocalDate date = LocalDate.now().minusMonths(1);
        return date.getYear() * 100 + date.getMonthValue();
    }
}
