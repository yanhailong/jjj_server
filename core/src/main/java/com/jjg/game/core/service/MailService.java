package com.jjg.game.core.service;

import com.jjg.game.core.dao.MailDao;
import com.jjg.game.core.data.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/11 17:41
 */
@Service
public class MailService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MailDao mailDao;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private RedisTemplate<String, Long> redisTemplate;
    @Autowired
    private PlayerSessionService playerSessionService;

    /**
     * 获取玩家的所有邮件
     * @param playerId
     * @return
     */
    public List<Mail> getMailByPlayerId(long playerId) {
        return mailDao.getMailByPlayerId(playerId);
    }

    /**
     * 删除邮件
     * @param playerId
     * @param mailId
     */
    public void removeMail(long playerId,int mailId) {
        mailDao.removeMail(playerId,mailId);
    }
}
