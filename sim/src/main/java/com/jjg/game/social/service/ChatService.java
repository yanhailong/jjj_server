package com.jjg.game.social.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.social.channel.ChatChannel;
import com.jjg.game.social.channel.ChatChannelRegistry;
import com.jjg.game.social.channel.ChatHistory;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.data.ChatMessage;
import com.jjg.game.social.pb.SocialPbConverter;
import com.jjg.game.social.pb.res.ResChatHistory;
import com.jjg.game.social.pb.res.ResSendChat;
import com.jjg.game.social.pb.struct.ChatMsgInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天发送/拉取编排。
 * <p>
 * 统一做 空内容/字数/频率/敏感词 校验, 再委托给具体 {@link ChatChannel}; 系统消息由服务端直接下发。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatChannelRegistry registry;
    @Autowired
    private SocialRateLimiter rateLimiter;
    @Autowired
    private SnowflakeManager snowflakeManager;

    /**
     * 玩家发送聊天。
     */
    public ResSendChat sendChat(PlayerController pc, int channelCode, long targetId, String content) {
        ResSendChat res = new ResSendChat(Code.SUCCESS);
        try {
            ChatChannel channel = registry.get(channelCode);
            if (channel == null || !channel.clientSendable()) {
                res.code = Code.PARAM_ERROR;
                log.warn("发送聊天信息失败, channel匹配失败 playerId={},channelCode={}", pc.playerId(), channelCode);
                return res;
            }
            if (content == null || content.trim().isEmpty()) {
                res.code = Code.PARAM_ERROR;
                log.warn("发送聊天信息失败, 内容不能为空 playerId={},channelCode={}", pc.playerId(), channelCode);
                return res;
            }
            content = content.trim();
            int maxLen = channel.maxContentLength();
            if (maxLen > 0 && content.length() > maxLen) {
                res.code = Code.PARAM_ERROR;
                log.warn("发送聊天信息失败, 内容长度超过限制 playerId={},channelCode={},len={},maxLen={}", pc.playerId(), channelCode, content.length(), maxLen);
                return res;
            }

            Player sender = pc.getPlayer();
            int vcode = channel.validate(sender, targetId, content);
            if (vcode != Code.SUCCESS) {
                res.code = Code.PARAM_ERROR;
                log.warn("发送聊天信息失败, 频道校验失败 playerId={},channelCode={}", pc.playerId(), channelCode);
                return res;
            }
            //全部校验通过后才占用频率配额, 避免无效发送也被惩罚
            if (!rateLimiter.tryAcquire(pc.playerId(), channelCode, channel.sendIntervalMs())) {
                res.code = Code.PARAM_ERROR;
                log.warn("发送聊天信息失败, 发送频率超过限制 playerId={},channelCode={}", pc.playerId(), channelCode);
                return res;
            }
            //频道全局配额 (全服扇出型频道封顶总量); 放在个人限频之后, 刷屏请求不空耗全局配额
            if (!channel.tryAcquireGlobalQuota()) {
                res.code = Code.PARAM_ERROR;
                log.warn("发送聊天信息失败, 频道全局限流 playerId={},channelCode={}", pc.playerId(), channelCode);
                return res;
            }

            ChatMessage msg = buildMessage(channelCode, sender, targetId, content);
            channel.dispatch(msg);
            res.msg = SocialPbConverter.toChatMsgInfo(msg);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 拉取频道历史。
     */
    public ResChatHistory pullHistory(long playerId, int channelCode, long targetId, long cursor) {
        ResChatHistory res = new ResChatHistory(Code.SUCCESS);
        try {
            ChatChannel channel = registry.get(channelCode);
            if (channel == null) {
                res.code = Code.PARAM_ERROR;
                log.warn("拉取频道历史失败, channel匹配失败 playerId={},channelCode={}", playerId, channelCode);
                return res;
            }
            ChatHistory history = channel.loadHistory(playerId, targetId, cursor > 0 ? String.valueOf(cursor) : null);
            res.channel = channelCode;
            res.cursor = (history.nextCursor == null || history.nextCursor.isEmpty()) ? 0 : Long.parseLong(history.nextCursor);
            List<ChatMsgInfo> list = new ArrayList<>(history.list.size());
            for (ChatMessage m : history.list) {
                list.add(SocialPbConverter.toChatMsgInfo(m));
            }
            res.list = list;
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 服务端下发系统消息 (运营公告/大奖播报等)。
     */
    public void sendSystemMessage(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        ChatChannel channel = registry.get(ChatChannelType.SYSTEM);
        if (channel == null) {
            return;
        }
        ChatMessage msg = new ChatMessage();
        msg.setId(snowflakeManager.nextId());
        msg.setChannel(ChatChannelType.SYSTEM.getCode());
        msg.setFromId(0);
        msg.setContent(content);
        msg.setTime(System.currentTimeMillis());
        channel.dispatch(msg);
    }

    private ChatMessage buildMessage(int channelCode, Player sender, long targetId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setId(snowflakeManager.nextId());
        msg.setChannel(channelCode);
        msg.setFromId(sender.getId());
        msg.setFromNick(sender.getNickName());
        msg.setFromHeadImg(sender.getHeadImgId());
        msg.setFromHeadFrame(sender.getHeadFrameId());
        msg.setToId(targetId);
        msg.setContent(content);
        msg.setTime(System.currentTimeMillis());
        return msg;
    }
}
