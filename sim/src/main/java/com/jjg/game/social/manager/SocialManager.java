package com.jjg.game.social.manager;

import com.jjg.game.social.service.ChatService;
import com.jjg.game.social.service.PrivateChatService;
import com.jjg.game.social.service.SystemMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 社交模块管理器: 生命周期与对外入口。
 * <p>
 * 由 HallStartManager 在启动/停止时调用 {@link #init()}/{@link #shutdown()} (与 simManager 一致)。
 * 频道注册由 {@code ChatChannelRegistry} 自动完成, 这里只负责需要显式管理的资源(私聊 IO 线程/TTL 索引)。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class SocialManager {
    private static final Logger log = LoggerFactory.getLogger(SocialManager.class);

    @Autowired
    private PrivateChatService privateChatService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private SystemMessageService systemMessageService;

    public void init() {
        privateChatService.init();
        systemMessageService.init();
        log.info("社交模块初始化完成");
    }

    public void shutdown() {
        privateChatService.shutdown();
        log.info("社交模块已关闭");
    }

    /**
     * 下发系统消息(全服)。供 GM / 运营 / 预留的 ToSocialBridge 调用。
     */
    public void sendSystemMessage(String content) {
        chatService.sendSystemMessage(content);
    }
}
