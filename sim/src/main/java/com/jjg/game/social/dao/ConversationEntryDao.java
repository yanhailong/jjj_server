package com.jjg.game.social.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.social.data.ConversationEntry;
import com.jjg.game.social.data.PrivateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 私聊会话摘要 DAO。
 *
 * @author 11
 * @date 2026/6/10
 */
@Repository
public class ConversationEntryDao extends MongoBaseDao<ConversationEntry, String> {
    private static final Logger log = LoggerFactory.getLogger(ConversationEntryDao.class);

    public ConversationEntryDao(@Autowired MongoTemplate mongoTemplate) {
        super(ConversationEntry.class, mongoTemplate);
    }

    /**
     * 确保索引存在 (会话列表查询 + TTL 自动过期)。启动时调用。
     */
    public void ensureIndexes(int keepDays) {
        try {
            mongoTemplate.indexOps(ConversationEntry.class)
                    .ensureIndex(new Index().on("playerId", Sort.Direction.ASC).on("lastTime", Sort.Direction.DESC));
            mongoTemplate.indexOps(ConversationEntry.class)
                    .ensureIndex(new Index().on("updateTime", Sort.Direction.ASC).expire(Duration.ofDays(keepDays)));
        } catch (Exception e) {
            log.error("创建会话摘要索引失败", e);
        }
    }

    /**
     * 某玩家的会话摘要 (最新消息倒序)。
     */
    public List<ConversationEntry> listByPlayer(long playerId, int limit) {
        Query q = new Query(Criteria.where("playerId").is(playerId))
                .with(Sort.by(Sort.Direction.DESC, "lastTime"))
                .limit(limit);
        return mongoTemplate.find(q, ConversationEntry.class);
    }

    /**
     * 清零未读 (打开会话 / 单向删除会话时)。
     *
     * @return 清零前的未读数
     */
    public int resetUnread(long playerId, long targetId) {
        Query q = new Query(Criteria.where("_id").is(ConversationEntry.id(playerId, targetId)));
        q.fields().include("unread");
        ConversationEntry entry = mongoTemplate.findAndModify(q, new Update().set("unread", 0),
                FindAndModifyOptions.options().returnNew(false), ConversationEntry.class);
        return entry == null ? 0 : entry.getUnread();
    }

    /**
     * 把一批已落库的私聊消息增量应用到双侧会话摘要 (一次 bulk upsert)。
     * <p>
     * 同一会话同侧在批内聚合: last* 取批内最大 id 的消息, unread 累加接收侧未读条数
     * (进缓冲后已被"打开会话"标为已读的不计)。批量落库由单线程串行执行且消息 id 全局递增,
     * 故跨批 $set last* 单调向前; 缓冲满降级的同步落库与之并发时 last* 可能短暂回退,
     * 下一条消息即修正, 可接受。
     */
    public void bulkApply(Collection<PrivateMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        Map<String, SideAgg> aggs = new LinkedHashMap<>();
        for (PrivateMessage m : messages) {
            //发送侧: 只刷新最新消息
            accumulate(aggs, m.getFromId(), m.getToId(), m, 0);
            //接收侧: 刷新最新消息 + 未读计数
            accumulate(aggs, m.getToId(), m.getFromId(), m, m.isRead() ? 0 : 1);
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ConversationEntry.class);
        Date now = new Date();
        for (SideAgg agg : aggs.values()) {
            Update update = new Update()
                    .set("playerId", agg.ownerId)
                    .set("targetId", agg.targetId)
                    .set("lastMsgId", agg.last.getId())
                    .set("lastFromId", agg.last.getFromId())
                    .set("lastContent", agg.last.getContent())
                    .set("lastTime", agg.last.getTime())
                    .set("updateTime", now);
            if (agg.unreadInc > 0) {
                update.inc("unread", agg.unreadInc);
            }
            bulk.upsert(new Query(Criteria.where("_id").is(ConversationEntry.id(agg.ownerId, agg.targetId))), update);
        }
        bulk.execute();
    }

    private void accumulate(Map<String, SideAgg> aggs, long ownerId, long targetId, PrivateMessage msg, int unreadInc) {
        SideAgg agg = aggs.computeIfAbsent(ConversationEntry.id(ownerId, targetId), k -> new SideAgg(ownerId, targetId));
        if (agg.last == null || msg.getId() > agg.last.getId()) {
            agg.last = msg;
        }
        agg.unreadInc += unreadInc;
    }

    /**
     * 单侧会话在一批消息内的聚合结果。
     */
    private static class SideAgg {
        final long ownerId;
        final long targetId;
        PrivateMessage last;
        int unreadInc;

        SideAgg(long ownerId, long targetId) {
            this.ownerId = ownerId;
            this.targetId = targetId;
        }
    }
}
