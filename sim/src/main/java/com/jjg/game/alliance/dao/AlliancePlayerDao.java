package com.jjg.game.alliance.dao;

import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.data.PlayerTakenTask;
import com.jjg.game.core.dao.MongoBaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家侧联盟数据 DAO。
 * <p>
 * "每人只能在 1 个联盟"的约束以本文档 allianceId 字段的"0 -> aid"条件占位实现:
 * 加入/创建先占位、联盟文档写入失败再回滚, 两文档间不需要事务。
 * 每日计数等玩家私有字段因请求按 playerId 串行, 走普通 set; 贡献值扣减保留条件 $inc 兜底。
 *
 * @author 11
 * @date 2026/6/11
 */
@Repository
public class AlliancePlayerDao extends MongoBaseDao<AlliancePlayerData, Long> {

    public AlliancePlayerDao(@Autowired MongoTemplate mongoTemplate) {
        super(AlliancePlayerData.class, mongoTemplate);
    }

    private Query byId(long playerId) {
        return new Query(Criteria.where("_id").is(playerId));
    }

    private void ensurePlayerDocument(long playerId) {
        mongoTemplate.upsert(byId(playerId),
                new Update().setOnInsert("_id", playerId), AlliancePlayerData.class);
    }

    private void resetDailyCounter(long playerId, int day, String dayField, String countField) {
        ensurePlayerDocument(playerId);
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("_id").is(playerId),
                new Criteria().orOperator(
                        Criteria.where(dayField).exists(false),
                        Criteria.where(dayField).ne(day))));
        mongoTemplate.updateFirst(query,
                new Update().set(dayField, day).set(countField, 0), AlliancePlayerData.class);
    }

    private boolean tryIncrementDailyCounter(long playerId, int day, String dayField, String countField, int limit) {
        if (limit <= 0) {
            return false;
        }
        resetDailyCounter(playerId, day, dayField, countField);
        Query query = new Query(Criteria.where("_id").is(playerId)
                .and(dayField).is(day)
                .and(countField).lt(limit));
        return mongoTemplate.updateFirst(query,
                new Update().inc(countField, 1), AlliancePlayerData.class).getModifiedCount() > 0;
    }

    private AlliancePlayerData incrementDailyCounterReturning(long playerId, int day,
                                                              String dayField, String countField, int limit) {
        if (limit <= 0) {
            return null;
        }
        resetDailyCounter(playerId, day, dayField, countField);
        Query query = new Query(Criteria.where("_id").is(playerId)
                .and(dayField).is(day)
                .and(countField).lt(limit));
        return mongoTemplate.findAndModify(query, new Update().inc(countField, 1),
                FindAndModifyOptions.options().returnNew(true), AlliancePlayerData.class);
    }

    private void rollbackDailyCounter(long playerId, int day, String dayField, String countField) {
        Query query = new Query(Criteria.where("_id").is(playerId)
                .and(dayField).is(day)
                .and(countField).gt(0));
        mongoTemplate.updateFirst(query, new Update().inc(countField, -1), AlliancePlayerData.class);
    }

    /**
     * 读取; 不存在则返回仅含 playerId 的空对象 (不落库)。
     */
    public AlliancePlayerData getOrEmpty(long playerId) {
        AlliancePlayerData data = findById(playerId).orElse(null);
        if (data == null) {
            data = new AlliancePlayerData();
            data.setPlayerId(playerId);
        }
        return data;
    }

    // ----------------------- 联盟占位 (加入/创建/退出) -----------------------

    /**
     * 原子占位加入联盟: 仅当当前无盟 (allianceId 不存在或为 0) 时写入。
     * <p>
     * upsert + 条件不匹配时按 _id 插入触发唯一键冲突的范式与 {@code FriendDao#addRequestIfAbsent}
     * 一致: 冲突可能是"已在盟中"也可能是"并发首次建文档", 重试一次即可区分。
     *
     * @return true 占位成功
     */
    public boolean tryOccupy(long playerId, long allianceId, long joinTime) {
        Query query = new Query(Criteria.where("_id").is(playerId)
                .orOperator(Criteria.where("allianceId").exists(false), Criteria.where("allianceId").is(0)));
        Update update = new Update().set("allianceId", allianceId).set("joinTime", joinTime);
        for (int i = 0; i < 2; i++) {
            try {
                var result = mongoTemplate.upsert(query, update, AlliancePlayerData.class);
                //命中已有文档(修改) 或 首次建文档(插入) 均视为占位成功
                return result.getModifiedCount() > 0 || result.getUpsertedId() != null;
            } catch (DuplicateKeyException ignore) {
                //文档已存在且条件不匹配(已在盟中), 或并发首次创建文档的瞬时冲突 (重试一次区分)
            }
        }
        return false;
    }

    /**
     * 创建联盟占位: 在 {@link #tryOccupy} 基础上额外要求从未创建过联盟。
     *
     * @return true 占位成功
     */
    public boolean tryOccupyCreate(long playerId, long allianceId, long joinTime) {
        Query query = new Query(Criteria.where("_id").is(playerId)
                .andOperator(
                        new Criteria().orOperator(Criteria.where("allianceId").exists(false), Criteria.where("allianceId").is(0)),
                        new Criteria().orOperator(Criteria.where("createdAllianceId").exists(false), Criteria.where("createdAllianceId").is(0))));
        Update update = new Update()
                .set("allianceId", allianceId)
                .set("createdAllianceId", allianceId)
                .set("joinTime", joinTime);
        for (int i = 0; i < 2; i++) {
            try {
                var result = mongoTemplate.upsert(query, update, AlliancePlayerData.class);
                return result.getModifiedCount() > 0 || result.getUpsertedId() != null;
            } catch (DuplicateKeyException ignore) {
                //已在盟中/已创建过, 或并发首次创建文档的瞬时冲突 (重试一次区分)
            }
        }
        return false;
    }

    /**
     * 清除联盟占位 (退出/被踢/解散/加入回滚): 仅当当前确实在该联盟时清零, 防误清新盟。
     */
    public boolean clearAlliance(long playerId, long allianceId) {
        Query query = new Query(Criteria.where("_id").is(playerId).and("allianceId").is(allianceId));
        return mongoTemplate.updateFirst(query,
                new Update().set("allianceId", 0L), AlliancePlayerData.class).getModifiedCount() > 0;
    }

    /**
     * 批量清除占位 (解散联盟): 同样带 allianceId 条件防误清。
     */
    public void clearAllianceBulk(Collection<Long> playerIds, long allianceId) {
        if (playerIds == null || playerIds.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, AlliancePlayerData.class);
        for (Long pid : playerIds) {
            bulk.updateOne(new Query(Criteria.where("_id").is(pid).and("allianceId").is(allianceId)),
                    new Update().set("allianceId", 0L));
        }
        bulk.execute();
    }

    // ----------------------- 贡献值 -----------------------

    /**
     * 累加贡献值 (任务/捐献/帮助产出), 同时累计历史贡献度。
     */
    public void addContribution(long playerId, long contribution, long contributionTotal) {
        Update update = new Update().inc("contribution", contribution);
        if (contributionTotal > 0) {
            update.inc("contributionTotal", contributionTotal);
        }
        mongoTemplate.upsert(byId(playerId), update, AlliancePlayerData.class);
    }

    /**
     * 条件扣减贡献值: 余额充足才扣 (兜底跨入口并发)。
     *
     * @return true 扣减成功
     */
    public boolean tryDeductContribution(long playerId, long cost) {
        Query query = new Query(Criteria.where("_id").is(playerId).and("contribution").gte(cost));
        return mongoTemplate.updateFirst(query,
                new Update().inc("contribution", -cost), AlliancePlayerData.class).getModifiedCount() > 0;
    }

    // ----------------------- 每日计数 -----------------------

    public void setDonate(long playerId, int day, int count) {
        mongoTemplate.upsert(byId(playerId),
                new Update().set("donateDay", day).set("donateCount", count), AlliancePlayerData.class);
    }

    public int reserveDonate(long playerId, int day, int limit) {
        AlliancePlayerData data = incrementDailyCounterReturning(playerId, day, "donateDay", "donateCount", limit);
        return data == null ? -1 : data.getDonateCount();
    }

    public void rollbackDonate(long playerId, int day) {
        rollbackDailyCounter(playerId, day, "donateDay", "donateCount");
    }

    public void setTaskFinish(long playerId, int day, int count) {
        mongoTemplate.upsert(byId(playerId),
                new Update().set("taskDay", day).set("taskFinishCount", count), AlliancePlayerData.class);
    }

    public void incrementTaskFinish(long playerId, int day) {
        resetDailyCounter(playerId, day, "taskDay", "taskFinishCount");
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(playerId).and("taskDay").is(day)),
                new Update().inc("taskFinishCount", 1), AlliancePlayerData.class);
    }

    public void setSeekHelp(long playerId, int day, int count) {
        mongoTemplate.upsert(byId(playerId),
                new Update().set("seekHelpDay", day).set("seekHelpCount", count), AlliancePlayerData.class);
    }

    public boolean tryConsumeSeekHelp(long playerId, int day, int limit) {
        return tryIncrementDailyCounter(playerId, day, "seekHelpDay", "seekHelpCount", limit);
    }

    public void setHelp(long playerId, int day, int count) {
        mongoTemplate.upsert(byId(playerId),
                new Update().set("helpDay", day).set("helpCount", count), AlliancePlayerData.class);
    }

    public boolean tryConsumeHelp(long playerId, int day, int limit) {
        return tryIncrementDailyCounter(playerId, day, "helpDay", "helpCount", limit);
    }

    public void rollbackHelp(long playerId, int day) {
        rollbackDailyCounter(playerId, day, "helpDay", "helpCount");
    }

    /**
     * 写商店限购计数; 跨天首次购买时重置整个 map。
     */
    public void setShopPurchase(long playerId, int day, boolean newDay, int goodsId, int count) {
        Update update = new Update().set("shopDay", day);
        if (newDay) {
            Map<Integer, Integer> fresh = new HashMap<>();
            fresh.put(goodsId, count);
            update.set("shopPurchases", fresh);
        } else {
            update.set("shopPurchases." + goodsId, count);
        }
        mongoTemplate.upsert(byId(playerId), update, AlliancePlayerData.class);
    }

    public boolean tryPurchase(long playerId, int day, int goodsId, int dailyLimit, long price) {
        if (dailyLimit <= 0) {
            return false;
        }
        ensurePlayerDocument(playerId);
        Query resetQuery = new Query(new Criteria().andOperator(
                Criteria.where("_id").is(playerId),
                new Criteria().orOperator(
                        Criteria.where("shopDay").exists(false),
                        Criteria.where("shopDay").ne(day))));
        mongoTemplate.updateFirst(resetQuery,
                new Update().set("shopDay", day).set("shopPurchases", new HashMap<>()),
                AlliancePlayerData.class);

        String countField = "shopPurchases." + goodsId;
        Criteria limitCriteria = new Criteria().orOperator(
                Criteria.where(countField).exists(false),
                Criteria.where(countField).lt(dailyLimit));
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("_id").is(playerId),
                Criteria.where("shopDay").is(day),
                Criteria.where("contribution").gte(price),
                limitCriteria));
        Update update = new Update()
                .inc("contribution", -price)
                .inc(countField, 1);
        return mongoTemplate.updateFirst(query, update, AlliancePlayerData.class).getModifiedCount() > 0;
    }

    // ----------------------- 任务 -----------------------

    public void setTakenTask(long playerId, PlayerTakenTask task) {
        mongoTemplate.upsert(byId(playerId), new Update().set("takenTask", task), AlliancePlayerData.class);
    }

    /**
     * 仅取当前接取的任务快照 (投影): 进度上报(spin)高频路径的缓存回源用, 不拉整文档。
     *
     * @return 无文档/无任务返回 null
     */
    public PlayerTakenTask getTakenTask(long playerId) {
        Query query = byId(playerId);
        query.fields().include("takenTask");
        AlliancePlayerData data = mongoTemplate.findOne(query, AlliancePlayerData.class);
        return data == null ? null : data.getTakenTask();
    }

    public void clearTakenTask(long playerId) {
        mongoTemplate.updateFirst(byId(playerId), new Update().unset("takenTask"), AlliancePlayerData.class);
    }

    public boolean clearTakenTask(long playerId, long taskUid) {
        Query query = new Query(Criteria.where("_id").is(playerId).and("takenTask.uid").is(taskUid));
        return mongoTemplate.updateFirst(query,
                new Update().unset("takenTask"), AlliancePlayerData.class).getModifiedCount() > 0;
    }

    public void setAbandonCd(long playerId, long until) {
        mongoTemplate.upsert(byId(playerId), new Update().set("abandonCdUntil", until), AlliancePlayerData.class);
    }

    // ----------------------- 对决 -----------------------

    /**
     * 写阶段奖励领取位图; 顺带替换整个 map 以丢弃历史期数 (调用方只保留当期)。
     */
    public void setBattleClaim(long playerId, String period, int mask) {
        Map<String, Integer> claims = new HashMap<>();
        claims.put(period, mask);
        mongoTemplate.upsert(byId(playerId), new Update().set("battleClaims", claims), AlliancePlayerData.class);
    }

    // ----------------------- 查询 -----------------------

    /**
     * 批量读取 (成员列表展示贡献度等)。
     */
    public boolean tryClaimBattleStage(long playerId, String period, int stageMask) {
        ensurePlayerDocument(playerId);
        String field = "battleClaims." + period;
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("_id").is(playerId),
                new Criteria().orOperator(
                        Criteria.where(field).exists(false),
                        Criteria.where(field).bits().allClear(stageMask))));
        return mongoTemplate.updateFirst(query,
                new Update().bitwise(field).or(stageMask), AlliancePlayerData.class).getModifiedCount() > 0;
    }

    public Map<Long, AlliancePlayerData> multiGet(Collection<Long> playerIds) {
        Map<Long, AlliancePlayerData> map = new HashMap<>();
        if (playerIds == null || playerIds.isEmpty()) {
            return map;
        }
        Query query = new Query(Criteria.where("_id").in(playerIds));
        for (AlliancePlayerData data : mongoTemplate.find(query, AlliancePlayerData.class)) {
            map.put(data.getPlayerId(), data);
        }
        return map;
    }

    /**
     * 仅取 allianceId (投影): 高频"玩家在哪个盟"查询回源用。
     *
     * @return 所在联盟 id, 无文档/无盟返回 0
     */
    public long getAllianceId(long playerId) {
        Query query = byId(playerId);
        query.fields().include("allianceId");
        AlliancePlayerData data = mongoTemplate.findOne(query, AlliancePlayerData.class);
        return data == null ? 0 : data.getAllianceId();
    }

    /**
     * 取一批玩家的所在联盟 (对决结算/批量校验用)。
     */
    public Map<Long, Long> multiGetAllianceId(Collection<Long> playerIds) {
        Map<Long, Long> map = new HashMap<>();
        if (playerIds == null || playerIds.isEmpty()) {
            return map;
        }
        Query query = new Query(Criteria.where("_id").in(playerIds));
        query.fields().include("allianceId");
        for (AlliancePlayerData data : mongoTemplate.find(query, AlliancePlayerData.class)) {
            map.put(data.getPlayerId(), data.getAllianceId());
        }
        return map;
    }

    /**
     * 取某联盟全部成员的玩家侧数据 (解散前/结算用)。
     */
    public List<AlliancePlayerData> findByAllianceId(long allianceId) {
        return mongoTemplate.find(new Query(Criteria.where("allianceId").is(allianceId)), AlliancePlayerData.class);
    }
}
