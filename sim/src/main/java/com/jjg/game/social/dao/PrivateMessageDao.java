package com.jjg.game.social.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.social.data.PrivateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * 私聊消息 DAO。
 *
 * @author 11
 * @date 2026/6/9
 */
@Repository
public class PrivateMessageDao extends MongoBaseDao<PrivateMessage, Long> {
    private static final Logger log = LoggerFactory.getLogger(PrivateMessageDao.class);

    public PrivateMessageDao(@Autowired MongoTemplate mongoTemplate) {
        super(PrivateMessage.class, mongoTemplate);
    }

    /**
     * 确保 TTL 索引存在 (createTime, expireAfter = keepDays)。启动时调用。
     * <p>
     * 注: 会话列表改读摘要表后不再需要 fromId/_id、toId/_id、(toId,read,conversationId) 三个索引,
     * 这里不再创建; 既有环境的存量索引需手动 dropIndex 以减少写放大。
     */
    public void ensureTtlIndex(int keepDays) {
        try {
            mongoTemplate.indexOps(PrivateMessage.class)
                    .ensureIndex(new Index().on("createTime", Sort.Direction.ASC).expire(Duration.ofDays(keepDays)));
            //会话分页索引
            mongoTemplate.indexOps(PrivateMessage.class)
                    .ensureIndex(new Index().on("conversationId", Sort.Direction.ASC).on("_id", Sort.Direction.DESC));
            //打开会话标记已读 (markRead) 索引
            mongoTemplate.indexOps(PrivateMessage.class)
                    .ensureIndex(new Index().on("conversationId", Sort.Direction.ASC).on("toId", Sort.Direction.ASC).on("read", Sort.Direction.ASC));
        } catch (Exception e) {
            log.error("创建私聊索引失败", e);
        }
    }

    public void insertMessage(PrivateMessage message) {
        mongoTemplate.insert(message);
    }

    /**
     * 批量落库 (写缓冲定时刷盘用)。
     * 用按 _id 的 upsert(replaceOne) 而非 insert, 保证<b>幂等</b>: 落库失败重投不会因主键冲突再次失败, 可安全重试。
     */
    public void insertBatch(Collection<PrivateMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, PrivateMessage.class);
        FindAndReplaceOptions upsert = FindAndReplaceOptions.options().upsert();
        for (PrivateMessage m : messages) {
            bulk.replaceOne(new Query(Criteria.where("_id").is(m.getId())), m, upsert);
        }
        bulk.execute();
    }

    /**
     * 按会话分页拉取 (最新在前)。
     *
     * @param conversationId 会话id
     * @param cursorId       游标 (上一页最旧消息id, 0 表示取最新一页)
     * @param minTime        仅取此时间(ms)之后的消息 (单向删除会话的清除点; 0 表示不限)
     * @param size           每页条数
     */
    public List<PrivateMessage> page(String conversationId, long cursorId, long minTime, int size) {
        Criteria c = Criteria.where("conversationId").is(conversationId);
        if (cursorId > 0) {
            c.and("_id").lt(cursorId);
        }
        if (minTime > 0) {
            c.and("time").gt(minTime);
        }
        Query q = new Query(c).with(Sort.by(Sort.Direction.DESC, "_id")).limit(size);
        return mongoTemplate.find(q, PrivateMessage.class);
    }

    /**
     * 标记某会话中指定时间前发给"我"的消息为已读。
     */
    public void markRead(String conversationId, long toId, long maxTime) {
        Query q = new Query(Criteria.where("conversationId").is(conversationId)
                .and("toId").is(toId)
                .and("read").is(false)
                .and("time").lte(maxTime));
        mongoTemplate.updateMulti(q, new Update().set("read", true), PrivateMessage.class);
    }

}
