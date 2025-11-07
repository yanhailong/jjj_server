package com.jjg.game.core.dao;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Mail;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 11
 * @date 2025/8/11 17:22
 */
@Repository
public class MailDao extends MongoBaseDao<Mail, Long> {
    private Logger log = LoggerFactory.getLogger(getClass());

    public MailDao(@Autowired MongoTemplate mongoTemplate) {
        super(Mail.class, mongoTemplate);
    }

    private final String serverMailTableName = "serverMail";

    private final String playerServerMailTableName = "playerServerMail";
    @Autowired
    private RedisTemplate redisTemplate;

    private String getPlayerServerMailTableName(long mailId) {
        return playerServerMailTableName + ":" + mailId;
    }

    /**
     * 获取玩家的所有邮件
     *
     * @param playerId
     * @return
     */
    public List<Mail> getMailsByPlayerId(long playerId, int page, int size) {
        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
        }

        Query query = Query.query(Criteria.where("playerId").is(playerId))
                .with(Sort.by(Sort.Direction.DESC, "sendTime")) // 按发送时间降序排列
                .skip(page * size)
                .limit(size);
        return mongoTemplate.find(query, Mail.class);
    }

    /**
     * 获取玩家的一封邮件
     *
     * @param playerId
     * @return
     */
    public Mail getMailByPlayerId(long playerId, long id) {
        return mongoTemplate.findOne(Query.query(Criteria.where("id").is(id).and("playerId").is(playerId)), Mail.class);
    }

    /**
     * 删除已读邮件,和已领取的邮件
     *
     * @param playerId
     * @return
     */
    public long removeReadMails(long playerId) {
        // 创建查询条件
        Criteria criteria = Criteria.where("playerId").is(playerId)
                .andOperator(
                        new Criteria().orOperator(
                                Criteria.where("status").is(GameConstant.Mail.STATUS_READ)
                                        .andOperator(
                                                new Criteria().orOperator(
                                                        Criteria.where("items").size(0),
                                                        Criteria.where("items").is(null)
                                                )
                                        ),
                                Criteria.where("status").is(GameConstant.Mail.STATUS_GET_ITEMS)
                        )
                );
        Query query = new Query(criteria);
        // 执行删除操作
        return mongoTemplate.remove(query, Mail.class).getDeletedCount();
    }

    /**
     * 获取未领取道具的邮件
     *
     * @param playerId
     * @return
     */
    public List<Mail> getItemMails(long playerId, int status) {
        // 创建查询条件
        Criteria criteria = Criteria.where("playerId").is(playerId).and("status").ne(status)
                .and("items").exists(true)
                .ne(null)
                .not().size(0);

        Query query = new Query(criteria);
        return mongoTemplate.find(query, Mail.class);
    }

    /**
     * 获取邮件数量
     */
    public long getItemsMailsCount(long playerId) {
        Criteria criteria = Criteria.where("playerId").is(playerId).andOperator(
                new Criteria().orOperator(
                        Criteria.where("status").is(GameConstant.Mail.STATUS_NOT_READ),
                        new Criteria().andOperator(
                                Criteria.where("status").is(GameConstant.Mail.STATUS_READ),
                                Criteria.where("items").exists(true).ne(null).not().size(0)
                        )
                )
        );
        Query query = new Query(criteria);
        return mongoTemplate.count(query, Mail.class);
    }

    /**
     * 批量获取玩家的邮件数据
     * @param playerIds 玩家ids
     * @return 全部邮件信息
     */
    public List<Mail> getItemsMailsCount(Collection<Long> playerIds) {
        Criteria criteria = Criteria.where("playerId").in(playerIds).andOperator(
                new Criteria().orOperator(
                        Criteria.where("status").is(GameConstant.Mail.STATUS_NOT_READ),
                        new Criteria().andOperator(
                                Criteria.where("status").is(GameConstant.Mail.STATUS_READ),
                                Criteria.where("items").exists(true).ne(null).not().size(0)
                        )
                )
        );
        Query query = new Query(criteria);
        return mongoTemplate.find(query, Mail.class);
    }

    /**
     * @param mailId
     */
    public boolean readMail(long playerId, long mailId) {
        Query query = Query.query(Criteria.where("id").is(mailId)
                .and("playerId").is(playerId)
                .and("status").lt(GameConstant.Mail.STATUS_READ));
        return mongoTemplate.updateFirst(query, Update.update("status", GameConstant.Mail.STATUS_READ), Mail.class).getModifiedCount() > 0;
    }

    /**
     * 修改邮件状态
     *
     * @param mailId
     */
    public boolean getMailItems(long playerId, long mailId) {
        Query query = Query.query(Criteria.where("id").is(mailId)
                .and("playerId").is(playerId)
                .and("status").lte(GameConstant.Mail.STATUS_READ));

        return mongoTemplate.updateFirst(query, Update.update("status", GameConstant.Mail.STATUS_GET_ITEMS), Mail.class).getModifiedCount() > 0;
    }

    /**
     * 删除邮件
     *
     * @param playerId
     * @param mailId
     */
    public void removeMail(long playerId, long mailId) {
        mongoTemplate.remove(Query.query(Criteria.where("id").is(mailId).and("playerId").is(playerId)), Mail.class);
    }

    /**
     * 保存邮件
     */
    public void addMail(Mail mail) {
        mongoTemplate.save(mail);
    }

    /**
     * 批量更新邮件状态
     *
     * @param mailIds
     * @param newStatus
     * @return
     */
    public long batchUpdateMailStatus(List<Long> mailIds, int newStatus) {
        if (mailIds == null || mailIds.isEmpty()) {
            return 0;
        }

        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, Mail.class);

        // 为每个邮件ID添加更新操作
        for (long mailId : mailIds) {
            Query query = new Query(Criteria.where("id").is(mailId));
            Update update = new Update().set("status", newStatus);
            bulkOps.updateOne(query, update);
        }

        // 执行批量操作
        BulkWriteResult result = bulkOps.execute();
        return result.getModifiedCount();
    }

    /**
     * 使用BulkOperations批量保存Mail对象
     *
     * @param mails 要保存的Mail对象列表
     * @return 操作影响的文档数量
     */
    public long batchSaveMails(List<Mail> mails) {
        if (mails == null || mails.isEmpty()) {
            return 0;
        }

        BulkOperations bulkOps = mongoTemplate.bulkOps(
                BulkOperations.BulkMode.ORDERED,
                Mail.class
        );

        // 添加所有插入操作
        for (Mail mail : mails) {
            bulkOps.insert(mail);
        }

        // 执行批量操作
        BulkWriteResult result = bulkOps.execute();
        return result.getInsertedCount();
    }

    /**
     * 获取全服邮件
     *
     * @return
     */
    public List<Mail> getServerMails() {
        return redisTemplate.opsForHash().values(serverMailTableName);
    }

    /**
     * 获取全服邮件
     *
     * @param mailId
     * @return
     */
    public Mail getServerMailById(long mailId) {
        return (Mail) redisTemplate.opsForHash().get(serverMailTableName, mailId);
    }

    /**
     * 删除一个全服邮件
     *
     * @param mailId
     * @return
     */
    public void removeServerMail(long mailId) {
        redisTemplate.opsForHash().delete(serverMailTableName, mailId);
        redisTemplate.delete(getPlayerServerMailTableName(mailId));
    }

    /**
     * 清除过期邮件
     */
    public void cleanMails() {
        int now = TimeHelper.nowInt();
        //清除已到期的
        DeleteResult deleteResult = mongoTemplate.remove(Query.query(Criteria.where("timeout").lt(now).gt(0)), Mail.class);// 删除没有附件且超时的邮件

        //清除系统邮件
        Set<Long> removeSet = new HashSet<>();
        List<Mail> mailList = getServerMails();
        for (Mail mail : mailList) {
            if (mail.getTimeout() != -1 && mail.getTimeout() < now) {
                removeSet.add(mail.getId());
            }
        }

        long deletedServermails = 0;
        if (!removeSet.isEmpty()) {
            deletedServermails = redisTemplate.opsForHash().delete(serverMailTableName, removeSet.toArray());
        }
        log.info("删除玩家过期邮件数 : {} , 删除过期全服邮件数: {}", deleteResult.getDeletedCount(), deletedServermails);
    }

    /**
     * 更新过期时间
     *
     * @param mailId
     * @param timeout
     */
    public void updateTimeout(long mailId, long timeout) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(mailId)), Update.update("timeout", timeout), Mail.class);
    }

    /**
     * 保存全服邮件
     *
     * @param mail
     */
    public void saveServerMail(Mail mail) {
        redisTemplate.opsForHash().put(serverMailTableName, mail.getId(), mail);
    }

    /**
     * 添加玩家已领取的全服邮件id
     *
     * @param playerId
     * @param mailId
     */
    public void addPlayerServerMail(long playerId, long mailId) {
        redisTemplate.opsForSet().add(getPlayerServerMailTableName(mailId), playerId);
    }

    /**
     * 添加玩家已领取的全服邮件id
     *
     * @param mailId
     */
    public void addPlayersServerMail(Set<Long> playerIdSet, long mailId) {
        redisTemplate.opsForSet().add(getPlayerServerMailTableName(mailId), playerIdSet.toArray());
    }

    /**
     * 检查玩家是否领取过该全服邮件
     *
     * @param playerId
     * @param mailId
     * @return
     */
    public boolean playerHasServerMail(long playerId, long mailId) {
        return redisTemplate.opsForSet().isMember(getPlayerServerMailTableName(mailId), playerId);
    }
}
