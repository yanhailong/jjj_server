package com.jjg.game.social.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.social.data.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 玩家定向系统消息 DAO。
 */
@Repository
public class SystemMessageDao extends MongoBaseDao<SystemMessage, Long> {
    private static final Logger log = LoggerFactory.getLogger(SystemMessageDao.class);

    public SystemMessageDao(@Autowired MongoTemplate mongoTemplate) {
        super(SystemMessage.class, mongoTemplate);
    }

    public void ensureTtlIndex(int keepDays) {
        try {
            mongoTemplate.indexOps(SystemMessage.class)
                    .ensureIndex(new Index().on("createTime", Sort.Direction.ASC).expire(Duration.ofDays(keepDays)));
        } catch (Exception e) {
            log.error("创建玩家系统消息 TTL 索引失败", e);
        }
    }

    /**
     * 主键是玩家 id，并发或重复注册回调也只保留第一条欢迎消息。
     */
    public void insertIfAbsent(SystemMessage message) {
        try {
            mongoTemplate.insert(message);
        } catch (DuplicateKeyException ignore) {
            //已存在即表示该玩家的欢迎消息已经创建
        }
    }
}
