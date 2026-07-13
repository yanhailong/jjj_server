package com.jjg.game.season.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.data.SeasonMatchRecord;
import com.jjg.game.season.data.SeasonPendingSettlement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 玩家赛季聚合 DAO。
 */
@Repository
public class SeasonPlayerDao extends MongoBaseDao<SeasonPlayerData, Long> {
    private static final Logger log = LoggerFactory.getLogger(SeasonPlayerDao.class);

    /**
     * 待结算记录 TTL 保留天数; 赛季最长 14 天, 超过该时长的记录必然对应已切季的 seasonKey, 不再可消费。
     */
    private static final int PENDING_TTL_DAYS = 30;

    public SeasonPlayerDao(@Autowired MongoTemplate mongoTemplate) {
        super(SeasonPlayerData.class, mongoTemplate);
        ensureIndexes();
    }

    /**
     * 匹配/排行/待结算查询都会跨全表, 无索引时是集合扫描; 启动时确保索引存在。
     */
    private void ensureIndexes() {
        try {
            //匹配候选查询: seasonKey + representativeGameType + representativeStake
            mongoTemplate.indexOps(SeasonPlayerData.class).ensureIndex(new Index()
                    .on("seasonKey", Sort.Direction.ASC)
                    .on("representativeGameType", Sort.Direction.ASC)
                    .on("representativeStake", Sort.Direction.ASC));
            //总榜排序与名次 count: seasonKey + seasonCoin desc + totalEarnedCoin desc + playerId
            mongoTemplate.indexOps(SeasonPlayerData.class).ensureIndex(new Index()
                    .on("seasonKey", Sort.Direction.ASC)
                    .on("seasonCoin", Sort.Direction.DESC)
                    .on("totalEarnedCoin", Sort.Direction.DESC)
                    .on("playerId", Sort.Direction.ASC));
            //待结算按玩家消费
            mongoTemplate.indexOps(SeasonPendingSettlement.class).ensureIndex(new Index()
                    .on("playerId", Sort.Direction.ASC)
                    .on("seasonKey", Sort.Direction.ASC));
            //TTL 兜底清理不可消费的遗留记录
            mongoTemplate.indexOps(SeasonPendingSettlement.class).ensureIndex(new Index()
                    .on("createTime", Sort.Direction.ASC).expire(Duration.ofDays(PENDING_TTL_DAYS)));
        } catch (Exception e) {
            log.error("创建赛季索引失败", e);
        }
    }

    public void saveAll(Collection<SeasonPlayerData> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SeasonPlayerData.class);
        FindAndReplaceOptions upsert = FindAndReplaceOptions.options().upsert();
        for (SeasonPlayerData data : values) {
            bulk.replaceOne(Query.query(Criteria.where("_id").is(data.getPlayerId())), data, upsert);
        }
        bulk.execute();
    }

    /**
     * 只读取匹配所需字段，避免把对手完整历史拉入内存。
     * {@code minSpins} 下推到查询 (第 minSpins-1 个数组元素存在即长度足够), 不合格文档不出库。
     */
    public List<SeasonPlayerData> findMatchCandidates(String seasonKey, long excludePlayerId,
                                                       int gameType, long stake, int minSpins, int limit) {
        Query query = Query.query(Criteria.where("seasonKey").is(seasonKey)
                .and("playerId").ne(excludePlayerId)
                .and("representativeGameType").is(gameType)
                .and("representativeStake").is(stake)
                .and("representativeSpinWins." + (Math.max(1, minSpins) - 1)).exists(true)
                .and("seasonCoin").gt(0));
        query.fields().include("playerId", "playerName", "headImgId", "headFrameId", "tierId", "seasonKey",
                "seasonCoin", "representativeGameType", "representativeStake", "representativeSpinWins");
        query.limit(limit);
        return mongoTemplate.find(query, SeasonPlayerData.class);
    }

    /**
     * 将被挑战方的结果写入独立幂等队列，避免覆盖其所在节点的内存聚合。
     */
    public boolean applyOpponentSettlement(long playerId, String seasonKey, String matchId,
                                           long coinDelta, SeasonMatchRecord record, long historyLimit) {
        String id = playerId + ":" + matchId;
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update()
                .setOnInsert("playerId", playerId)
                .setOnInsert("seasonKey", seasonKey)
                .setOnInsert("matchId", matchId)
                .setOnInsert("coinDelta", coinDelta)
                .setOnInsert("record", record)
                .setOnInsert("historyLimit", Math.toIntExact(historyLimit))
                .setOnInsert("createTime", new Date());
        return mongoTemplate.upsert(query, update, SeasonPendingSettlement.class).getUpsertedId() != null;
    }

    public List<SeasonPendingSettlement> findPendingSettlements(long playerId, String seasonKey) {
        Query query = Query.query(Criteria.where("playerId").is(playerId).and("seasonKey").is(seasonKey));
        query.with(Sort.by(Sort.Order.asc("createTime")));
        return mongoTemplate.find(query, SeasonPendingSettlement.class);
    }

    public void deletePendingSettlements(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        mongoTemplate.remove(Query.query(Criteria.where("_id").in(ids)), SeasonPendingSettlement.class);
    }

    /**
     * 切季时清理该玩家名下所有待结算记录: 旧 seasonKey 的记录已在切季前消费,
     * 之后并发写入的旧季记录不再可消费, 直接丢弃 (赛季截止后送达的对局结果不跨季生效)。
     */
    public void deletePendingSettlementsByPlayer(long playerId) {
        mongoTemplate.remove(Query.query(Criteria.where("playerId").is(playerId)), SeasonPendingSettlement.class);
    }

    public List<SeasonPlayerData> findRanking(String seasonKey, int limit) {
        Query query = Query.query(Criteria.where("seasonKey").is(seasonKey));
        query.with(Sort.by(Sort.Order.desc("seasonCoin"), Sort.Order.desc("totalEarnedCoin"),
                Sort.Order.asc("playerId")));
        query.fields().include("playerId", "playerName", "seasonKey", "seasonCoin", "totalEarnedCoin", "tierId");
        query.limit(Math.max(1, limit));
        return mongoTemplate.find(query, SeasonPlayerData.class);
    }

    public long findRank(String seasonKey, long playerId, long seasonCoin, long totalEarnedCoin) {
        Criteria ahead = new Criteria().orOperator(
                Criteria.where("seasonCoin").gt(seasonCoin),
                new Criteria().andOperator(Criteria.where("seasonCoin").is(seasonCoin),
                        Criteria.where("totalEarnedCoin").gt(totalEarnedCoin)),
                new Criteria().andOperator(Criteria.where("seasonCoin").is(seasonCoin),
                        Criteria.where("totalEarnedCoin").is(totalEarnedCoin),
                        Criteria.where("playerId").lt(playerId)));
        Query query = Query.query(new Criteria().andOperator(Criteria.where("seasonKey").is(seasonKey), ahead));
        return mongoTemplate.count(query, SeasonPlayerData.class) + 1;
    }
}
