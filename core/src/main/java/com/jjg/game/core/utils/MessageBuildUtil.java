package com.jjg.game.core.utils;

import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.NoticeBaseInfoChange;

/**
 * 一些通用消息的构建
 *
 * @author lm
 * @date 2025/10/16 11:40
 */
public class MessageBuildUtil {

    /**
     * 构建玩家基本信息变化
     *
     * @param player 玩家信息
     * @return NoticeBaseInfoChange
     */
    public static NoticeBaseInfoChange buildNoticeBaseInfoChange(Player player) {
        NoticeBaseInfoChange notice = new NoticeBaseInfoChange();
        notice.gold = player.getGold();
        notice.diamond = player.getDiamond();
        notice.vipLevel = player.getVipLevel();
        notice.level = player.getLevel();
        notice.levelExp = player.getExp();
        return notice;
    }

}
