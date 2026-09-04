package com.jjg.game.ploy.games.mining;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.ploy.games.mining.message.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/** 排行投影可从已登记参赛者的背包恢复；赛季结束后冻结排序，邮件以业务键幂等发送。 */
@Service
public class MiningRankService {
    private static final Logger log = LoggerFactory.getLogger(MiningRankService.class);
    private final MongoTemplate mongo;
    private final RedissonClient redis;
    private final PlayerPackService packs;
    private final MailService mail;
    private final MiningConfig config;
    public MiningRankService(MongoTemplate mongo, RedissonClient redis, PlayerPackService packs, MailService mail, MiningConfig config) {
        this.mongo = mongo; this.redis = redis; this.packs = packs; this.mail = mail; this.config = config;
    }

    @Document("miningRank")
    @CompoundIndex(name = "season_depth_time_player", def = "{'seasonId':1,'depth':-1,'reachedAt':1,'playerId':1}")
    public static class Entry {
        @Id public String id;
        public String seasonId;
        public long playerId;
        public String nickName;
        public int headImgId;
        public int headFrameId;
        public int depth;
        public long reachedAt;
    }

    @Document("miningSettlement")
    public static class Settlement {
        @Id public String id;
        public long completedAt;
        public List<Award> awards = new ArrayList<>();
        public Set<Long> delivered = new HashSet<>();
    }

    public static class Award {
        public long playerId;
        public int rank;
        public Map<Integer, Long> items;
    }

    public void register(Player player, String seasonId) {
        Query query = Query.query(Criteria.where("_id").is(seasonId + ":" + player.getId()));
        mongo.upsert(query, new Update().setOnInsert("seasonId", seasonId).setOnInsert("playerId", player.getId())
                .setOnInsert("depth", 0).setOnInsert("reachedAt", 0)
                .set("nickName", player.getNickName()).set("headImgId", player.getHeadImgId())
                .set("headFrameId", player.getHeadFrameId()), Entry.class);
    }

    public void sync(Player player, MiningState state) {
        register(player, state.seasonId);
        sync(player.getId(), state);
    }

    public void sync(long playerId, MiningState state) {
        mongo.updateFirst(Query.query(Criteria.where("_id").is(state.seasonId + ":" + playerId)
                        .and("depth").lt(state.depth)),
                new Update().set("depth", state.depth).set("reachedAt", state.depthReachedAt), Entry.class);
    }

    public RLock seasonReadLock(String id) { return redis.getReadWriteLock("mining:season:" + id).readLock(); }

    public ResMiningRank rank(Player player, MiningConfig.Season season) {
        ResMiningRank res = new ResMiningRank(Code.SUCCESS);
        res.seasonId = season.id;
        List<Entry> entries = mongo.find(ranked(season.id).limit(300), Entry.class);
        res.ranks = new ArrayList<>();
        int position = 0;
        for (Entry entry : entries) res.ranks.add(info(entry, ++position, season));
        Entry self = mongo.findById(season.id + ":" + player.getId(), Entry.class);
        if (self == null || self.depth <= 0) {
            res.self = new MiningRankInfo(); res.self.playerId = player.getId(); res.self.rank = 0;
            res.self.nickName = player.getNickName(); res.self.headImgId = player.getHeadImgId();
            res.self.headFrameId = player.getHeadFrameId();
        } else {
            Criteria ahead = new Criteria().orOperator(Criteria.where("depth").gt(self.depth),
                    new Criteria().andOperator(Criteria.where("depth").is(self.depth), Criteria.where("reachedAt").lt(self.reachedAt)),
                    new Criteria().andOperator(Criteria.where("depth").is(self.depth), Criteria.where("reachedAt").is(self.reachedAt),
                            Criteria.where("playerId").lt(self.playerId)));
            long count = mongo.count(Query.query(new Criteria().andOperator(Criteria.where("seasonId").is(season.id), ahead)), Entry.class);
            res.self = info(self, Math.toIntExact(count + 1), season);
        }
        return res;
    }

    private Query ranked(String seasonId) {
        return Query.query(Criteria.where("seasonId").is(seasonId).and("depth").gt(0))
                .with(Sort.by(Sort.Order.desc("depth"), Sort.Order.asc("reachedAt"), Sort.Order.asc("playerId")));
    }

    private MiningRankInfo info(Entry entry, int rank, MiningConfig.Season season) {
        MiningRankInfo info = new MiningRankInfo();
        info.rank = rank; info.playerId = entry.playerId; info.nickName = entry.nickName;
        info.headImgId = entry.headImgId; info.headFrameId = entry.headFrameId; info.depth = entry.depth;
        info.rewards = ItemUtils.buildItemInfo(reward(season, rank));
        return info;
    }

    static Map<Integer, Long> reward(MiningConfig.Season season, int rank) {
        return season.rewards.stream().filter(r -> rank >= r.from && rank <= r.to).findFirst().map(r -> r.items).orElse(Map.of());
    }

    @Scheduled(fixedDelay = 60000)
    public void settleEndedSeasons() {
        long now = System.currentTimeMillis();
        try { config.currentSeason(now); }
        catch (RuntimeException e) { log.error("挖矿赛季配置无效，停止结算", e); return; }
        for (MiningConfig.Season season : config.seasons) {
            if (season.endTime <= 0 || season.endTime > now) continue;
            RLock lock = redis.getReadWriteLock("mining:season:" + season.id).writeLock();
            if (!lock.tryLock()) continue;
            try {
                Settlement settled = mongo.findById(season.id, Settlement.class);
                if (settled != null && settled.completedAt > 0) continue;
                if (settled == null) {
                    // 修复提交存档后、更新排行前宕机留下的投影缺口。
                    try (var entries = mongo.stream(Query.query(Criteria.where("seasonId").is(season.id)), Entry.class)) {
                        entries.forEach(entry -> {
                            PlayerPack pack = packs.getFromAllDB(entry.playerId);
                            if (pack == null || pack.getMiningState() == null) return;
                            MiningState state = JSON.parseObject(pack.getMiningState(), MiningState.class);
                            if (season.id.equals(state.seasonId)) sync(entry.playerId, state);
                        });
                    }
                    settled = new Settlement(); settled.id = season.id;
                    int rank = 0;
                    for (Entry entry : mongo.find(ranked(season.id).limit(300), Entry.class)) {
                        Map<Integer, Long> reward = reward(season, ++rank);
                        if (reward.isEmpty()) continue;
                        Award award = new Award(); award.playerId = entry.playerId; award.rank = rank; award.items = new HashMap<>(reward);
                        settled.awards.add(award);
                    }
                    // 发奖前冻结名次与奖励；失败重试不受随后配置变更影响。
                    mongo.insert(settled);
                }
                for (Award award : settled.awards) {
                    if (settled.delivered.contains(award.playerId)) continue;
                    List<Item> items = award.items.entrySet().stream().map(e -> new Item(e.getKey(), e.getValue())).toList();
                    if (!mail.addMailIfAbsent(award.playerId, "挖矿赛季奖励", "赛季 " + season.id + " 深度排行第 " + award.rank + " 名", items,
                            AddType.MINING_RANK, "mining:rank:" + season.id + ":" + award.playerId))
                        throw new IllegalStateException("Failed mining rank mail");
                    settled.delivered.add(award.playerId);
                    mongo.save(settled);
                }
                settled.completedAt = now;
                mongo.save(settled);
            } catch (Exception e) {
                log.error("挖矿赛季结算失败，将重试 season={}", season.id, e);
            } finally { lock.unlock(); }
        }
    }
}
