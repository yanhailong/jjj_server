package com.jjg.game.core.utils;

import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.ActivityItemDropInfo;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;

import java.util.Map;

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
        notice.vipLevel = player.getVipLevel();
        notice.level = player.getLevel();
        notice.levelExp = player.getExp();
        return notice;
    }

    /**
     * 构建活动掉落信息
     *
     * @param activityType 活动类型
     * @param activityId   活动id
     * @param gameCfgId    房间配置id
     * @param dropItems    合并后掉落的道具
     * @return
     */
    public static ActivityItemDropInfo buildActivityDropInfo(int activityType, long activityId, int gameCfgId, Map<Integer, Long> dropItems) {
        ActivityItemDropInfo activityItemDropInfo = new ActivityItemDropInfo();
        activityItemDropInfo.activityType = activityType;
        activityItemDropInfo.activityId = activityId;
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(gameCfgId);
        activityItemDropInfo.gameType = warehouseCfg.getGameType();
        activityItemDropInfo.itemMap = ItemUtils.buildItemInfo(dropItems);
        return activityItemDropInfo;
    }

}
