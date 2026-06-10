package com.jjg.game.social.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.social.data.FriendData;
import com.jjg.game.social.data.FriendEntry;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 好友数据 DAO。
 * <p>
 * 全部写操作走 Mongo 字段级原子更新(upsert), 跨玩家写无需载入对方文档, 避免并发竞态与丢更新。
 *
 * @author 11
 * @date 2026/6/9
 */
@Repository
public class FriendDao extends MongoBaseDao<FriendData, Long> {

    public FriendDao(@Autowired MongoTemplate mongoTemplate) {
        super(FriendData.class, mongoTemplate);
    }

    private Query byId(long playerId) {
        return new Query(Criteria.where("playerId").is(playerId));
    }

    /**
     * 读取; 不存在则返回一个仅含 playerId 的空对象 (不落库)。
     */
    public FriendData getOrEmpty(long playerId) {
        FriendData data = findById(playerId).orElse(null);
        if (data == null) {
            data = new FriendData();
            data.setPlayerId(playerId);
        }
        return data;
    }

    // ----------------------- 好友 -----------------------

    /**
     * 仅取好友 id 集合 (投影 friends 字段, 不读全文档)。供状态广播等只关心好友关系的场景。
     */
    public Set<Long> getFriendIds(long playerId) {
        Query query = byId(playerId);
        query.fields().include("friends");
        FriendData data = mongoTemplate.findOne(query, FriendData.class);
        if (data == null || data.getFriends() == null || data.getFriends().isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(data.getFriends().keySet());
    }

    /**
     * 一次聚合取一批玩家的好友数量 (服务端只回传计数, 不拉文档)。
     * 无文档者不在结果中, 调用方按 0 处理。
     */
    public Map<Long, Integer> friendCounts(Collection<Long> playerIds) {
        Map<Long, Integer> map = new HashMap<>();
        if (playerIds == null || playerIds.isEmpty()) {
            return map;
        }
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("playerId").in(playerIds)),
                context -> new Document("$project", new Document("count",
                        new Document("$size", new Document("$objectToArray",
                                new Document("$ifNull", Arrays.asList("$friends", new Document())))))));
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, FriendData.class, Document.class);
        for (Document doc : results) {
            Number id = (Number) doc.get("_id");
            if (id == null) {
                continue;
            }
            Number cnt = (Number) doc.get("count");
            map.put(id.longValue(), cnt == null ? 0 : cnt.intValue());
        }
        return map;
    }

    public void addFriend(long playerId, long friendId, FriendEntry entry) {
        mongoTemplate.upsert(byId(playerId), new Update().set("friends." + friendId, entry), FriendData.class);
    }

    public void removeFriend(long playerId, long friendId) {
        mongoTemplate.updateFirst(byId(playerId), new Update().unset("friends." + friendId), FriendData.class);
    }

    public void removeFriends(long playerId, Collection<Long> friendIds) {
        if (friendIds == null || friendIds.isEmpty()) {
            return;
        }
        Update update = new Update();
        for (Long id : friendIds) {
            update.unset("friends." + id);
        }
        mongoTemplate.updateFirst(byId(playerId), update, FriendData.class);
    }

    /**
     * 双向批量删除好友: "我"的文档一次性 unset 全部好友, 再用 bulk 从每个好友文档移除我。
     * 由 N*2 次单独更新收敛为 1 次单文档更新 + 1 次 bulk。
     */
    public void removeFriendsBidirectional(long selfId, Collection<Long> friendIds) {
        if (friendIds == null || friendIds.isEmpty()) {
            return;
        }
        Update selfUpdate = new Update();
        for (Long fid : friendIds) {
            selfUpdate.unset("friends." + fid);
        }
        mongoTemplate.updateFirst(byId(selfId), selfUpdate, FriendData.class);

        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, FriendData.class);
        for (Long fid : friendIds) {
            bulk.updateOne(byId(fid), new Update().unset("friends." + selfId));
        }
        bulk.execute();
    }

    public void setFriendGiftDay(long playerId, long friendId, int day) {
        mongoTemplate.updateFirst(byId(playerId), new Update().set("friends." + friendId + ".lastGiftSendDay", day), FriendData.class);
    }

    // ----------------------- 申请 -----------------------

    /**
     * 原子写入好友申请: 仅当该申请尚不存在时写入 (一次条件 upsert, 替代"先查重再写"的两次往返)。
     * <p>
     * 申请已存在时条件不匹配, upsert 转为按 _id 插入触发唯一键冲突;
     * 并发首次创建文档也会偶发同样的冲突, 故冲突后重试一次以区分两种情况。
     *
     * @return true 写入成功; false 该申请已在对方待处理中
     */
    public boolean addRequestIfAbsent(long targetId, long requesterId, long time) {
        Query query = new Query(Criteria.where("playerId").is(targetId)
                .and("pendingRequests." + requesterId).exists(false));
        Update update = new Update().set("pendingRequests." + requesterId, time);
        for (int i = 0; i < 2; i++) {
            try {
                mongoTemplate.upsert(query, update, FriendData.class);
                return true;
            } catch (DuplicateKeyException ignore) {
                //已申请过, 或并发创建文档的瞬时冲突(重试一次即可区分)
            }
        }
        return false;
    }

    public boolean hasPendingRequest(long targetId, long requesterId) {
        Query query = new Query(Criteria.where("playerId").is(targetId)
                .and("pendingRequests." + requesterId).exists(true));
        return mongoTemplate.exists(query, FriendData.class);
    }

    /**
     * 待处理申请数 (一次聚合只回传计数, 不拉文档)。供发起申请时校验对方待处理上限。
     */
    public int pendingRequestCount(long playerId) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("playerId").is(playerId)),
                context -> new Document("$project", new Document("count",
                        new Document("$size", new Document("$objectToArray",
                                new Document("$ifNull", Arrays.asList("$pendingRequests", new Document())))))));
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, FriendData.class, Document.class);
        Document doc = results.getUniqueMappedResult();
        if (doc == null) {
            return 0;
        }
        Number cnt = (Number) doc.get("count");
        return cnt == null ? 0 : cnt.intValue();
    }

    public void removeRequest(long playerId, long requesterId) {
        mongoTemplate.updateFirst(byId(playerId), new Update().unset("pendingRequests." + requesterId), FriendData.class);
    }

    public void removeRequests(long playerId, Collection<Long> requesterIds) {
        if (requesterIds == null || requesterIds.isEmpty()) {
            return;
        }
        Update update = new Update();
        for (Long id : requesterIds) {
            update.unset("pendingRequests." + id);
        }
        mongoTemplate.updateFirst(byId(playerId), update, FriendData.class);
    }

    public void setDailyRequest(long playerId, int day, int count) {
        mongoTemplate.upsert(byId(playerId), new Update().set("dailyRequestDay", day).set("dailyRequestCount", count), FriendData.class);
    }

    /**
     * 一键处理好友申请的写入:
     * 在"我"的文档上一次性移除全部已处理申请(unset pendingRequests.*) + 写入已同意的好友(set friends.*);
     * 再用 bulk 给每个同意的好友写入反向好友关系。
     * 由 N(移除) + 2N(双向加好友) 次单独更新收敛为 1 次单文档更新 + 1 次 bulk。
     *
     * @param selfId               本人
     * @param handledRequesterIds  本次处理(同意或拒绝)的全部申请者id, 都从我的待处理中移除
     * @param acceptedFriends      已同意的好友 id -> 好友条目
     * @param now                  当前时间(ms), 用于反向好友条目
     */
    public void applyHandleRequest(long selfId, Collection<Long> handledRequesterIds,
                                   Map<Long, FriendEntry> acceptedFriends, long now) {
        Update selfUpdate = new Update();
        if (handledRequesterIds != null) {
            for (Long rid : handledRequesterIds) {
                selfUpdate.unset("pendingRequests." + rid);
            }
        }
        if (acceptedFriends != null) {
            for (Map.Entry<Long, FriendEntry> en : acceptedFriends.entrySet()) {
                selfUpdate.set("friends." + en.getKey(), en.getValue());
            }
        }
        mongoTemplate.updateFirst(byId(selfId), selfUpdate, FriendData.class);

        if (acceptedFriends == null || acceptedFriends.isEmpty()) {
            return;
        }
        //反向: 给每个同意的好友写入我(其文档可能尚不存在, 用 upsert)
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, FriendData.class);
        for (Long rid : acceptedFriends.keySet()) {
            bulk.upsert(byId(rid), new Update().set("friends." + selfId, new FriendEntry(now)));
        }
        bulk.execute();
    }

    // ----------------------- 黑名单 -----------------------


    public Set<Long> getBlacklistIds(long playerId) {
        Query query = byId(playerId);
        query.fields().include("blacklist");
        FriendData data = mongoTemplate.findOne(query, FriendData.class);
        if (data == null || data.getBlacklist() == null || data.getBlacklist().isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(data.getBlacklist().keySet()));
    }

    /**
     * 批量加入黑名单 (一次单文档更新)。
     */
    public void addBlacklists(long playerId, Collection<Long> blackIds, long time) {
        if (blackIds == null || blackIds.isEmpty()) {
            return;
        }
        Update update = new Update();
        for (Long id : blackIds) {
            update.set("blacklist." + id, time);
        }
        mongoTemplate.upsert(byId(playerId), update, FriendData.class);
    }

    public void removeBlacklist(long playerId, long blackId) {
        mongoTemplate.updateFirst(byId(playerId), new Update().unset("blacklist." + blackId), FriendData.class);
    }

    /**
     * 批量移出黑名单 (一次单文档更新)。
     */
    public void removeBlacklists(long playerId, Collection<Long> blackIds) {
        if (blackIds == null || blackIds.isEmpty()) {
            return;
        }
        Update update = new Update();
        for (Long id : blackIds) {
            update.unset("blacklist." + id);
        }
        mongoTemplate.updateFirst(byId(playerId), update, FriendData.class);
    }

    public void clearBlacklist(long playerId) {
        mongoTemplate.updateFirst(byId(playerId), new Update().set("blacklist", new HashMap<>()), FriendData.class);
    }

    // ----------------------- 私聊会话清除标记 -----------------------

    /**
     * 设置某会话的单向清除时间 (该时间点之前的消息对本人不再展示, 不影响对方)。
     * 顺带清理早于 expireBefore 的过期清除标记 —— 消息本身有 TTL, 比消息保留期还老的标记已无意义,
     * 借助本低频写入口防止 conversationClear 无界增长。
     *
     * @param expireBefore 早于该时间(ms)的旧标记一并 unset; <=0 表示不清理
     */
    public void setConversationClear(long playerId, long targetId, long time, long expireBefore) {
        Update update = new Update().set("conversationClear." + targetId, time);
        if (expireBefore > 0) {
            Map<Long, Long> existing = getConversationClear(playerId);
            for (Map.Entry<Long, Long> en : existing.entrySet()) {
                if (en.getKey() != targetId && en.getValue() != null && en.getValue() < expireBefore) {
                    update.unset("conversationClear." + en.getKey());
                }
            }
        }
        mongoTemplate.upsert(byId(playerId), update, FriendData.class);
    }

    /**
     * 仅取私聊会话清除标记 (投影, 不读全文档)。供拉历史/会话列表用。
     */
    public Map<Long, Long> getConversationClear(long playerId) {
        Query query = byId(playerId);
        query.fields().include("conversationClear");
        FriendData data = mongoTemplate.findOne(query, FriendData.class);
        if (data == null || data.getConversationClear() == null) {
            return Collections.emptyMap();
        }
        return data.getConversationClear();
    }

    // ----------------------- 赠礼 -----------------------

    public void addPendingGift(long targetId, long senderId, int amount) {
        mongoTemplate.upsert(byId(targetId), new Update().set("pendingGifts." + senderId, amount), FriendData.class);
    }

    /**
     * 一键收送: 批量把"我"赠送的体力写入每个好友的待领, 并标记我对其的赠送日期。
     *
     * @param senderId 赠送者
     * @param targets  好友id列表
     * @param amount   每份体力数量
     * @param today    今日 yyyyMMdd
     */
    public void bulkSendGift(long senderId, Collection<Long> targets, int amount, int today) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, FriendData.class);
        List<Long> friendIds = new ArrayList<>(targets);
        //给每个好友写入待领赠礼
        for (Long targetId : friendIds) {
            bulk.upsert(byId(targetId), new Update().set("pendingGifts." + senderId, amount));
        }
        //更新自己对这些好友的赠送日期
        Update selfUpdate = new Update();
        for (Long targetId : friendIds) {
            selfUpdate.set("friends." + targetId + ".lastGiftSendDay", today);
        }
        bulk.updateOne(byId(senderId), selfUpdate);
        bulk.execute();
    }

    public void clearPendingGifts(long playerId) {
        mongoTemplate.updateFirst(byId(playerId), new Update().set("pendingGifts", new HashMap<>()), FriendData.class);
    }

    /**
     * 批量读取好友数据 (用于一次性取多名玩家的关系, 当前主要给信息卡/状态用)。
     */
    public Map<Long, FriendData> multiGet(Collection<Long> playerIds) {
        Map<Long, FriendData> map = new HashMap<>();
        if (playerIds == null || playerIds.isEmpty()) {
            return map;
        }
        Query query = new Query(Criteria.where("playerId").in(playerIds));
        for (FriendData data : mongoTemplate.find(query, FriendData.class)) {
            map.put(data.getPlayerId(), data);
        }
        return map;
    }
}
