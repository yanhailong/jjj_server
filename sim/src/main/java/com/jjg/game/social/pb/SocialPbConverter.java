package com.jjg.game.social.pb;

import com.jjg.game.core.data.Player;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.data.ChatMessage;
import com.jjg.game.social.data.PrivateMessage;
import com.jjg.game.social.pb.struct.BlacklistInfo;
import com.jjg.game.social.pb.struct.ChatMsgInfo;
import com.jjg.game.social.pb.struct.FriendInfo;
import com.jjg.game.social.pb.struct.RequestInfo;

/**
 * 社交 data/domain -> pb struct 转换器 (对齐 SimPbConverter 风格)。
 *
 * @author 11
 * @date 2026/6/9
 */
public final class SocialPbConverter {

    private SocialPbConverter() {
    }

    /**
     * 频道消息(世界/系统/联盟) -> ChatMsgInfo
     */
    public static ChatMsgInfo toChatMsgInfo(ChatMessage m) {
        ChatMsgInfo info = new ChatMsgInfo();
        info.id = m.getId();
        info.channel = m.getChannel();
        info.channelSubId = m.getChannelSubId();
        info.fromId = m.getFromId();
        info.fromNick = m.getFromNick();
        info.fromHeadImg = m.getFromHeadImg();
        info.fromHeadFrame = m.getFromHeadFrame();
        info.toId = m.getToId();
        info.content = m.getContent();
        info.time = m.getTime();
        return info;
    }

    /**
     * 私聊消息 -> ChatMsgInfo (发送者头像昵称由 from 填充)
     */
    public static ChatMsgInfo toChatMsgInfo(PrivateMessage m, Player from) {
        ChatMsgInfo info = new ChatMsgInfo();
        info.id = m.getId();
        info.channel = ChatChannelType.PRIVATE.getCode();
        info.fromId = m.getFromId();
        info.toId = m.getToId();
        info.content = m.getContent();
        info.time = m.getTime();
        if (from != null) {
            info.fromNick = from.getNickName();
            info.fromHeadImg = from.getHeadImgId();
            info.fromHeadFrame = from.getHeadFrameId();
        }
        return info;
    }

    public static FriendInfo toFriendInfo(Player p, int status, long offlineSeconds, int toadySendCount, boolean hasPendingGift) {
        FriendInfo info = new FriendInfo();
        if (p != null) {
            info.playerId = p.getId();
            info.nick = p.getNickName();
            info.headImg = p.getHeadImgId();
            info.headFrame = p.getHeadFrameId();
            info.level = p.getLevel();
        }
        info.status = status;
        info.offlineSeconds = offlineSeconds;
        info.toadySendCount = toadySendCount;
        info.hasPendingGift = hasPendingGift;
        return info;
    }

    public static RequestInfo toRequestInfo(Player p, long requestTime) {
        RequestInfo info = new RequestInfo();
        if (p != null) {
            info.playerId = p.getId();
            info.nick = p.getNickName();
            info.headImg = p.getHeadImgId();
            info.headFrame = p.getHeadFrameId();
            info.level = p.getLevel();
        }
        info.requestTime = requestTime;
        return info;
    }

    public static BlacklistInfo toBlacklistInfo(Player p) {
        BlacklistInfo info = new BlacklistInfo();
        if (p != null) {
            info.playerId = p.getId();
            info.nick = p.getNickName();
            info.headImg = p.getHeadImgId();
            info.headFrame = p.getHeadFrameId();
        }
        return info;
    }
}
