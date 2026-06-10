package com.jjg.game.social.channel;

import com.jjg.game.social.constant.ChatChannelType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天频道注册表。
 * <p>
 * Spring 启动时自动收集所有 {@link ChatChannel} 实现并按类型登记。新增频道实现接口即自动接入,
 * 无需改动本类或收发协议 —— 聊天系统扩展性的落点。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class ChatChannelRegistry {

    private final Map<ChatChannelType, ChatChannel> channels = new EnumMap<>(ChatChannelType.class);

    @Autowired
    public ChatChannelRegistry(List<ChatChannel> channelList) {
        for (ChatChannel channel : channelList) {
            channels.put(channel.type(), channel);
        }
    }

    public ChatChannel get(ChatChannelType type) {
        return type == null ? null : channels.get(type);
    }

    public ChatChannel get(int channelCode) {
        return get(ChatChannelType.of(channelCode));
    }
}
