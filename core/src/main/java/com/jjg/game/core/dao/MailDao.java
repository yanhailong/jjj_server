package com.jjg.game.core.dao;

import com.jjg.game.core.data.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/11 17:22
 */
@Repository
public class MailDao extends MongoBaseDao<Mail, Long>{
    private Logger log = LoggerFactory.getLogger(getClass());

    public MailDao(@Autowired MongoTemplate mongoTemplate) {
        super(Mail.class, mongoTemplate);
    }

    private final String serverMailTableName = "serverMail";
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取玩家的所有邮件
     * @param playerId
     * @return
     */
    public List<Mail> getMailByPlayerId(long playerId) {
        return mongoTemplate.find(Query.query(Criteria.where("playerId").is(playerId)), Mail.class);
    }

    /**
     * 修改邮件状态
     * @param mailId
     * @param status
     */
    public void updateStatus(long mailId, int status) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(mailId)), Update.update("status", status), Mail.class);
    }

    /**
     * 获取全服邮件
     * @return
     */
    public List<Mail> getServerMail() {
        return redisTemplate.opsForHash().values(serverMailTableName);
    }

    /**
     * 获取全服邮件
     * @param mailId
     * @return
     */
    public Mail getServerMailById(long mailId){
        return (Mail) redisTemplate.opsForHash().get(serverMailTableName,mailId);
    }

    /**
     * 删除一个全服邮件
     * @param mailId
     * @return
     */
    public void removeServerMail(long mailId){
        redisTemplate.opsForHash().delete(serverMailTableName,mailId);
    }

    /**
     * 清除过期邮件
     */
    public void cleanMail() {
        //清除已到期的
        mongoTemplate.remove(Query.query(Criteria.where("timeout").lt(System.currentTimeMillis()).gt(0)), Mail.class); // 删除没有附件且超时的邮件
        //清除系统邮件
        List<Mail> mailList = getServerMail();
        for (Mail mail : mailList) {
            if (mail.getTimeout() != -1 && mail.getTimeout() < System.currentTimeMillis()) {
                redisTemplate.opsForHash().delete(serverMailTableName, mail.getId());
            }
        }
    }

    /**
     * 更新过期时间
     * @param mailId
     * @param l
     */
    public void updateTimeout(long mailId, long l) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(mailId)), Update.update("timeout", l), Mail.class);
    }

    /**
     * 保存全服邮件
     * @param mail
     */
    public void saveServerMail(Mail mail) {
        redisTemplate.opsForHash().put(serverMailTableName, mail.getId(), mail);
    }

    /**
     * 将全服邮件迁移到redis
     */
    public void moveServerMailToRedis() {
        List<Mail> mailList = mongoTemplate.find(Query.query(Criteria.where("serverMail").is(true)), Mail.class);
        for (Mail mail : mailList) {
            saveServerMail(mail);
            log.info("系统邮件:mailId = {} , title = {}  迁移完成 ,", mail.getId(), mail.getTitle());
        }
    }
}
