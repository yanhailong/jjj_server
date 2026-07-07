package com.jjg.game.sim.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.RankEntry;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.RankService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PopularityRankingCfg;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.pb.res.ResVisitRank;
import com.jjg.game.sim.pb.struct.VisitRankInfo;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拜访人气自然月赛季榜。
 *
 * @author 11
 * @date 2026/6/30
 */
@Service
public class SimVisitRankService {
    private static final Logger log = LoggerFactory.getLogger(SimVisitRankService.class);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int SHOW_COUNT = 100;

    private final RankService rankService;
    private final CorePlayerService corePlayerService;
    private final SimCasinoDao simCasinoDao;
    private final MailService mailService;
    private final RedissonClient redissonClient;

    public SimVisitRankService(RankService rankService,
                               CorePlayerService corePlayerService,
                               SimCasinoDao simCasinoDao,
                               MailService mailService,
                               RedissonClient redissonClient) {
        this.rankService = rankService;
        this.corePlayerService = corePlayerService;
        this.simCasinoDao = simCasinoDao;
        this.mailService = mailService;
        this.redissonClient = redissonClient;
    }

    public long addPopularity(long playerId, int points) {
        if (playerId <= 0 || points <= 0) {
            return 0;
        }
        return rankService.addPoints(rankKey(LocalDate.now()), playerId, points);
    }

    public ResVisitRank buildRank(long playerId) {
        ResVisitRank res = new ResVisitRank(Code.SUCCESS);
        LocalDate today = LocalDate.now();
        String key = rankKey(today);
        List<RankEntry> entries = rankService.topN(key, SHOW_COUNT);
        List<Long> ids = entries.stream().map(RankEntry::getPlayerId).toList();
        Map<Long, Player> players = new HashMap<>(corePlayerService.multiGetPlayerMap(ids));
        Map<Long, Integer> casinoLevels = new HashMap<>(simCasinoDao.findMaxCasinoLevel(ids));
        List<VisitRankInfo> ranks = new ArrayList<>(entries.size());
        for (RankEntry entry : entries) {
            VisitRankInfo info = toInfo(entry, players, casinoLevels);
            if (info != null) {
                ranks.add(info);
            }
        }
        res.ranks = ranks;
        RankEntry myEntry = rankService.getRank(key, playerId);
        if (myEntry == null) {
            myEntry = new RankEntry(playerId, 0, -1);
        }
        if (!players.containsKey(playerId)) {
            players.putAll(corePlayerService.multiGetPlayerMap(List.of(playerId)));
        }
        if (!casinoLevels.containsKey(playerId)) {
            casinoLevels.putAll(simCasinoDao.findMaxCasinoLevel(List.of(playerId)));
        }
        res.my = toInfo(myEntry, players, casinoLevels);
        res.seasonEndTime = today.withDayOfMonth(1).plusMonths(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return res;
    }

    private VisitRankInfo toInfo(RankEntry entry, Map<Long, Player> players,
                                 Map<Long, Integer> casinoLevels) {
        Player player = players.get(entry.getPlayerId());
        if (player == null) {
            return null;
        }
        VisitRankInfo info = new VisitRankInfo();
        info.rank = (int) entry.getRank();
        info.playerId = entry.getPlayerId();
        info.playerName = player.getNickName();
        info.headImgId = player.getHeadImgId();
        info.headFrameId = player.getHeadFrameId();
        info.casinoLevel = casinoLevels.getOrDefault(entry.getPlayerId(), 0);
        info.popularity = entry.getPoints();
        Map<Integer, Long> rewards = rewardForRank(info.rank);
        info.rewards = rewards.isEmpty() ? Collections.emptyList() : ItemUtils.buildItemInfo(rewards);
        return info;
    }

    /**
     * leader 节点调用，按结算标记和锁保证同一赛季只发一次。
     */
    public void settlePreviousSeason() {
        LocalDate today = LocalDate.now();
        String oldKey = previousRankKey(today);
        String settledKey = oldKey + ":settled";
        RBucket<Boolean> marker = redissonClient.getBucket(settledKey);
        if (Boolean.TRUE.equals(marker.get())) {
            return;
        }
        RLock lock = redissonClient.getLock(oldKey + ":settle-lock");
        if (!lock.tryLock()) {
            return;
        }
        try {
            if (Boolean.TRUE.equals(marker.get())) {
                return;
            }
            List<RankEntry> entries = rankService.topN(oldKey, SHOW_COUNT);
            for (RankEntry entry : entries) {
                Map<Integer, Long> rewards = rewardForRank((int) entry.getRank());
                if (rewards.isEmpty()) {
                    continue;
                }
                List<Item> items = new ArrayList<>(rewards.size());
                rewards.forEach((id, count) -> items.add(new Item(id, count)));
                mailService.addMail(entry.getPlayerId(), "人气榜赛季奖励",
                        "上赛季人气榜第" + entry.getRank() + "名奖励", items,
                        AddType.SIM_VISIT_RANK_REWARD);
            }
            marker.set(true, Duration.ofDays(90));
            rankService.reset(oldKey);
            log.info("拜访人气榜赛季结算完成 key={},players={}", oldKey, entries.size());
        } catch (Exception e) {
            log.error("拜访人气榜赛季结算失败 key={}", oldKey, e);
        } finally {
            lock.unlock();
        }
    }

    static String rankKey(LocalDate day) {
        return "sim:visit:rank:" + MONTH.format(day);
    }

    static String previousRankKey(LocalDate day) {
        return rankKey(day.minusMonths(1));
    }

    /**
     * PopularityRanking 表 ranking 列: 单值为精确名次, 两值为名次区间 [from, to]。
     */
    static Map<Integer, Long> rewardForRank(int rank) {
        if (rank <= 0) {
            return Collections.emptyMap();
        }
        for (PopularityRankingCfg cfg : GameDataManager.getPopularityRankingCfgList()) {
            List<Integer> ranking = cfg.getRanking();
            if (ranking == null || ranking.isEmpty()) {
                continue;
            }
            int from = ranking.get(0);
            int to = ranking.size() > 1 ? ranking.get(1) : from;
            if (rank >= from && rank <= to) {
                Map<Integer, Long> rewards = cfg.getGetItem();
                return rewards == null ? Collections.emptyMap() : rewards;
            }
        }
        return Collections.emptyMap();
    }
}
