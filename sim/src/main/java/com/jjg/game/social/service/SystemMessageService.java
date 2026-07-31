package com.jjg.game.social.service;

import com.jjg.game.core.base.player.IPlayerRegister;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.dao.SystemMessageDao;
import com.jjg.game.social.data.ChatMessage;
import com.jjg.game.social.data.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 玩家定向系统消息的持久化与查询。
 */
@Component
public class SystemMessageService implements IPlayerRegister {

    @Autowired
    private SystemMessageDao systemMessageDao;
    @Autowired
    private SnowflakeManager snowflakeManager;

    public void init() {
        systemMessageDao.ensureTtlIndex(SocialConst.Cfg.SYSTEM_KEEP_DAYS);
    }

    @Override
    public void playerRegister(PlayerController playerController) {
        long now = System.currentTimeMillis();
        SystemMessage message = new SystemMessage();
        message.setPlayerId(playerController.playerId());
        message.setMessageId(snowflakeManager.nextId());
        message.setContent(SimConstant.Common.SYSTEM_WELCOME_MSG);
        message.setTime(now);
        message.setCreateTime(new Date(now));
        systemMessageDao.insertIfAbsent(message);
    }

    public ChatMessage getPlayerMessage(long playerId) {
        SystemMessage stored = systemMessageDao.findById(playerId).orElse(null);
        if (stored == null) {
            return null;
        }
        ChatMessage message = new ChatMessage();
        message.setId(stored.getMessageId());
        message.setChannel(ChatChannelType.SYSTEM.getCode());
        message.setFromId(0);
        message.setToId(playerId);
        message.setContent(stored.getContent());
        message.setTime(stored.getTime());
        return message;
    }
}
