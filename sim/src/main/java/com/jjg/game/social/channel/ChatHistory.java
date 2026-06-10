package com.jjg.game.social.channel;

import com.jjg.game.social.data.ChatMessage;

import java.util.Collections;
import java.util.List;

/**
 * 频道历史拉取结果。
 *
 * @author 11
 * @date 2026/6/9
 */
public class ChatHistory {
    //本次返回的消息 (按时间正序)
    public final List<ChatMessage> list;
    //下一页游标 (null/"" 表示无更早消息); 全服频道无分页时恒为空
    public final String nextCursor;

    public ChatHistory(List<ChatMessage> list, String nextCursor) {
        this.list = list == null ? Collections.emptyList() : list;
        this.nextCursor = nextCursor;
    }

    public static ChatHistory of(List<ChatMessage> list) {
        return new ChatHistory(list, null);
    }
}
