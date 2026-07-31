package com.jjg.game.alliance.pb;

import com.jjg.game.alliance.data.*;
import com.jjg.game.alliance.pb.struct.AllianceBrief;
import com.jjg.game.alliance.pb.struct.AllianceHelpOrderInfo;
import com.jjg.game.alliance.pb.struct.AllianceTaskInfo;
import com.jjg.game.alliance.pb.struct.ShowAllianceInfo;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AllianceLevelCfg;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.sim.service.SimConfigCacheService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 联盟域对象 -> PB 的无状态转换 (需要多源数据拼装的转换在各 service 内完成)。
 *
 * @author 11
 * @date 2026/6/11
 */
public class AlliancePbConverter {

    private AlliancePbConverter() {
    }

    /**
     * 联盟概要 (memberCap/nextLevelReputation 由配置推导, 调用方传入 config)
     */
    public static AllianceBrief toBrief(AllianceData data, SimConfigCacheService config) {
        if (data == null) {
            return null;
        }
        AllianceBrief brief = new AllianceBrief();
        fillBrief(brief, data, config);
        return brief;
    }

    /**
     * 可加入联盟信息 (概要 + 当前玩家是否已申请过)
     */
    public static ShowAllianceInfo toShowInfo(AllianceData data, SimConfigCacheService config, boolean applied) {
        if (data == null) {
            return null;
        }
        ShowAllianceInfo info = new ShowAllianceInfo();
        fillBrief(info, data, config);
        info.apply = applied;
        return info;
    }

    private static void fillBrief(AllianceBrief brief, AllianceData data, SimConfigCacheService config) {
        brief.allianceId = data.getAllianceId();
        brief.name = data.getName();
        brief.icon = data.getIcon();
        brief.notice = data.getNotice();
        brief.level = data.getLevel();
        brief.reputation = data.getReputation();

        AllianceLevelCfg cfg = config.allianceLevelCfg(data.getLevel());
        if (cfg != null) {
            brief.nextLevelReputation = cfg.getReputationRequired();
            brief.memberCap = cfg.getMaxMembers();
        }

        brief.memberCount = data.getMemberCount();
        brief.joinMinCasinoLevel = data.getJoinMinCasinoLevel();
        brief.joinNeedAudit = data.isJoinNeedAudit();
        brief.leaderId = data.getLeaderId();

        AllianceMember member = data.findMember(brief.leaderId);
        if (member != null) {
            brief.leaderPlayerName = member.getPlayerName();
        }
    }

    /**
     * 任务池条目 -> PB (未接取, 进度 0)
     */
    public static AllianceTaskInfo toTaskInfo(AllianceTaskSlot slot) {
        AllianceTaskInfo info = new AllianceTaskInfo();
        info.cfgId = slot.getCfgId();
        info.expireTime = slot.getExpireTime();

        TaskCfg taskCfg = GameDataManager.getTaskCfg(slot.getCfgId());
        if (taskCfg != null) {
            info.target = taskCfg.getTaskConditionId().get(taskCfg.getTaskConditionId().size() - 1);
            info.rewards = ItemUtils.buildItemInfo(taskCfg.getGetItem());
            info.langId = taskCfg.getLanguage();
            info.quality = taskCfg.getQuality();
            info.abandon = taskCfg.getAllowAbandon();
            info.conditionId = taskCfg.getTaskConditionId().get(0).intValue();
            info.duration = taskCfg.getDuration();
        }
        return info;
    }

    /**
     * 已接取任务 -> PB (带进度)
     */
    public static AllianceTaskInfo toTaskInfo(PlayerTakenTask task, long progress, TaskCfg taskCfg) {
        AllianceTaskInfo info = new AllianceTaskInfo();
        info.cfgId = task.getCfgId();
        info.expireTime = task.getExpireTime();
        info.progress = progress;
        info.beginTime = task.getAcceptTime();
        info.endTime = task.getFinishTime();

        if (taskCfg == null) {
            taskCfg = GameDataManager.getTaskCfg(task.getCfgId());
        }

        if (taskCfg != null) {
            info.target = taskCfg.getTaskConditionId().get(taskCfg.getTaskConditionId().size() - 1);
            info.rewards = ItemUtils.buildItemInfo(taskCfg.getGetItem());
            info.langId = taskCfg.getLanguage();
            info.quality = taskCfg.getQuality();
            info.abandon = taskCfg.getAllowAbandon();
            info.conditionId = taskCfg.getTaskConditionId().get(0).intValue();
            info.duration = taskCfg.getDuration();

        }
        return info;
    }

    /**
     * 互助订单 -> PB (ownerNick 由调用方批量查询后传入)
     */
    public static AllianceHelpOrderInfo toHelpOrderInfo(AllianceHelpOrder order, String ownerNick, long myId,
                                                        long endTime) {
        AllianceHelpOrderInfo info = new AllianceHelpOrderInfo();
        info.orderId = order.getOrderId();
        info.type = order.getType();
        info.ownerId = order.getOwnerId();
        info.ownerNick = ownerNick;
        info.targetId = order.getTargetId();
        info.targetName = order.getTargetName();
        info.helped = order.helpedCount();
        info.maxHelp = order.getMaxHelp();
        info.createTime = order.getCreateTime();
        info.endTime = endTime;
        info.myHelped = order.helpedBy(myId);
        return info;
    }

    /**
     * itemId -> count 的奖励 Map 转 PB 列表
     */
    public static List<ItemInfo> toItemInfos(Map<Integer, Long> items) {
        List<ItemInfo> list = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return list;
        }
        for (Map.Entry<Integer, Long> en : items.entrySet()) {
            ItemInfo info = new ItemInfo();
            info.itemId = en.getKey();
            info.count = en.getValue();
            list.add(info);
        }
        return list;
    }
}
