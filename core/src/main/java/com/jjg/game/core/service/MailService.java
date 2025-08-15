package com.jjg.game.core.service;

import com.jjg.game.core.constant.GameConstant;
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

    //每页数量
    private int mailPageSize = 50;

    /**
     * 获取玩家的邮件
     * @param playerId
     * @return
     */
    public List<Mail> getMailByPlayerId(long playerId,int page) {
        return mailDao.getMailsByPlayerId(playerId,page,mailPageSize);
    }

    /**
     * 阅读邮件
     * @param mailId
     */
    public boolean readMail(long playerId,int mailId) {
        return mailDao.updateStatus(playerId,mailId, GameConstant.Mail.STAUTS_READ);
    }

    /**
     * 领取邮件道具
     * @param mailId
     */
    public boolean getMailItems(long playerId,int mailId) {
        return mailDao.updateStatus(playerId,mailId, GameConstant.Mail.STAUTS_GET_ITEMS);
    }

    /**
     * 删除邮件
     * @param playerId
     * @param mailId
     */
    public void removeMail(long playerId,int mailId) {
        mailDao.removeMail(playerId,mailId);
    }

    /**
     * 获取玩家的一封邮件
     * @param playerId
     * @return
     */
    public Mail getMail(long playerId,int mailId) {
        return mailDao.getMailByPlayerId(playerId,mailId);
    }
}
