package com.jjg.game.alliance.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AllianceDao;
import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceApplication;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.data.DonateCfg;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.dao.RedDotReadDao;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.service.SimConfigCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 联盟总入口数字，以及免费捐献、待处理入盟申请、每日任务入口叶子红点。 */
@Service
public class AllianceRedDotService implements IRedDotService, SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(AllianceRedDotService.class);
    private static final String TASK_DAILY_ENTRY_SCOPE = "alliance_task_daily_entry";
    private static final List<Integer> RED_DOT_SUBMODULES = List.of(
            AllianceConst.RedDot.FREE_DONATE,
            AllianceConst.RedDot.APPLICATION,
            AllianceConst.RedDot.TASK_DAILY_ENTRY,
            AllianceConst.RedDot.ENTRANCE_TOTAL);

    @Autowired
    private AlliancePlayerDao alliancePlayerDao;
    @Autowired
    private AllianceDao allianceDao;
    @Autowired
    private SimConfigCacheService configService;
    @Autowired
    private SimPlayerContextRegistry contextRegistry;
    @Autowired
    private RedDotManager redDotManager;
    @Autowired
    private RedDotReadDao redDotReadDao;
    @Autowired
    private AllianceTaskService allianceTaskService;

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.ALLIANCE;
    }

    @Override
    public List<Integer> getSubmodules() {
        return RED_DOT_SUBMODULES;
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        List<Integer> submodules = submodule == 0 ? RED_DOT_SUBMODULES : List.of(submodule);
        if (submodule != 0 && !RED_DOT_SUBMODULES.contains(submodule)) {
            return List.of();
        }

        AlliancePlayerData playerData = alliancePlayerDao.findRedDotData(playerId);
        AllianceData alliance = playerData == null || playerData.getAllianceId() <= 0
                ? null : allianceDao.findRedDotData(playerData.getAllianceId());
        long now = System.currentTimeMillis();
        int today = TimeHelper.getDayNumerical();
        boolean needsTotal = submodules.contains(AllianceConst.RedDot.ENTRANCE_TOTAL);
        boolean freeDonation = (needsTotal || submodules.contains(AllianceConst.RedDot.FREE_DONATE))
                && hasFreeDonation(playerData, alliance, today);
        ApplicationSnapshot applicationSnapshot = needsTotal || submodules.contains(AllianceConst.RedDot.APPLICATION)
                ? applicationSnapshot(alliance, playerId, now) : ApplicationSnapshot.EMPTY;
        boolean taskReminder = (needsTotal || submodules.contains(AllianceConst.RedDot.TASK_DAILY_ENTRY))
                && playerData != null && playerData.getAllianceId() > 0
                && !redDotReadDao.viewedToday(playerId, TASK_DAILY_ENTRY_SCOPE)
                && allianceTaskService.hasAvailableTask(playerId);
        List<RedDotDetails> details = new ArrayList<>(submodules.size());
        for (int currentSubmodule : submodules) {
            if (currentSubmodule == AllianceConst.RedDot.FREE_DONATE) {
                details.add(buildDetails(currentSubmodule, freeDonation ? 1 : 0,
                        RedDotDetails.RedDotType.COMMON));
            } else if (currentSubmodule == AllianceConst.RedDot.APPLICATION) {
                details.add(buildDetails(currentSubmodule, applicationSnapshot.count(),
                        RedDotDetails.RedDotType.COUNT));
            } else if (currentSubmodule == AllianceConst.RedDot.TASK_DAILY_ENTRY) {
                details.add(buildDetails(currentSubmodule, taskReminder ? 1 : 0,
                        RedDotDetails.RedDotType.COMMON));
            } else if (currentSubmodule == AllianceConst.RedDot.ENTRANCE_TOTAL) {
                int freeDonationCount = freeDonation ? 1 : 0;
                int taskCount = taskReminder ? 1 : 0;
                int total = freeDonationCount + taskCount + applicationSnapshot.count();
                RedDotDetails totalDetails = buildDetails(currentSubmodule, total, RedDotDetails.RedDotType.COUNT);
                totalDetails.setExtra(JSON.toJSONString(Map.of(
                        "freeDonationCount", freeDonationCount,
                        "taskCount", taskCount,
                        "applicationCount", applicationSnapshot.count())));
                details.add(totalDetails);
            }
        }
        if (submodules.contains(AllianceConst.RedDot.FREE_DONATE)
                || submodules.contains(AllianceConst.RedDot.ENTRANCE_TOTAL)) {
            updateDonateDay(playerId, today);
        }
        if (submodules.contains(AllianceConst.RedDot.APPLICATION)
                || submodules.contains(AllianceConst.RedDot.ENTRANCE_TOTAL)) {
            updateApplicationExpireTime(playerId, applicationSnapshot.nextExpireTime());
        }
        if (submodules.contains(AllianceConst.RedDot.TASK_DAILY_ENTRY)
                || submodules.contains(AllianceConst.RedDot.ENTRANCE_TOTAL)) {
            updateTaskDay(playerId, today);
        }
        return details;
    }

    @Override
    public boolean markRead(long playerId, int submodule, List<Integer> entityIds) {
        if (submodule != AllianceConst.RedDot.TASK_DAILY_ENTRY) {
            return false;
        }
        redDotReadDao.viewToday(playerId, TASK_DAILY_ENTRY_SCOPE);
        updateTaskDay(playerId, TimeHelper.getDayNumerical());
        refresh(playerId, AllianceConst.RedDot.ENTRANCE_TOTAL);
        return true;
    }

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        int today = TimeHelper.getDayNumerical();
        boolean refreshDonate = ctx.getAllianceDonateRedDotDay() != today;
        boolean refreshTask = ctx.getAllianceTaskRedDotDay() != today;
        long nextExpireTime = ctx.getAllianceApplicationRedDotNextExpireTime();
        boolean refreshApplications = nextExpireTime < 0 || nextExpireTime > 0 && now >= nextExpireTime;
        if (refreshDonate && refreshApplications && refreshTask) {
            refreshAll(ctx.playerId());
        } else {
            if (refreshDonate) {
                refresh(ctx.playerId(), AllianceConst.RedDot.FREE_DONATE);
            }
            if (refreshApplications) {
                refresh(ctx.playerId(), AllianceConst.RedDot.APPLICATION);
            }
            if (refreshTask) {
                refresh(ctx.playerId(), AllianceConst.RedDot.TASK_DAILY_ENTRY);
            }
        }
    }

    public void clearFreeDonation(long playerId) {
        refresh(playerId, AllianceConst.RedDot.FREE_DONATE);
    }

    public void refreshAll(long playerId) {
        try {
            redDotManager.updateRedDot(initialize(playerId, 0), playerId);
        } catch (Exception e) {
            log.error("刷新联盟全部红点异常 playerId={}", playerId, e);
        }
    }

    public void clearAll(long playerId) {
        updateDonateDay(playerId, TimeHelper.getDayNumerical());
        updateApplicationExpireTime(playerId, 0);
        push(playerId, List.of(
                buildDetails(AllianceConst.RedDot.FREE_DONATE, 0, RedDotDetails.RedDotType.COMMON),
                buildDetails(AllianceConst.RedDot.APPLICATION, 0, RedDotDetails.RedDotType.COUNT),
                buildDetails(AllianceConst.RedDot.TASK_DAILY_ENTRY, 0, RedDotDetails.RedDotType.COMMON),
                buildDetails(AllianceConst.RedDot.ENTRANCE_TOTAL, 0, RedDotDetails.RedDotType.COUNT)));
    }

    public void refreshApplications(long playerId) {
        refresh(playerId, AllianceConst.RedDot.APPLICATION);
    }

    public void refreshApplicationsForAlliance(long allianceId) {
        try {
            AllianceData alliance = allianceDao.findRedDotData(allianceId);
            if (alliance == null || alliance.getLeaderId() <= 0) {
                return;
            }
            long leaderId = alliance.getLeaderId();
            refresh(leaderId, AllianceConst.RedDot.APPLICATION);
        } catch (Exception e) {
            log.error("刷新联盟申请红点异常 allianceId={}", allianceId, e);
        }
    }

    private void refresh(long playerId, int submodule) {
        try {
            List<Integer> refreshSubmodules = submodule == AllianceConst.RedDot.ENTRANCE_TOTAL
                    ? List.of(submodule)
                    : List.of(submodule, AllianceConst.RedDot.ENTRANCE_TOTAL);
            redDotManager.updateRedDotByInitialize(getModule(), refreshSubmodules, playerId);
        } catch (Exception e) {
            log.error("刷新联盟红点异常 playerId={},submodule={}", playerId, submodule, e);
        }
    }

    private void push(long playerId, List<RedDotDetails> details) {
        try {
            redDotManager.updateRedDot(details, playerId);
        } catch (Exception e) {
            log.error("推送联盟红点异常 playerId={}", playerId, e);
        }
    }

    private boolean hasFreeDonation(AlliancePlayerData playerData, AllianceData alliance, int today) {
        DonateCfg cfg = configService.getAllianceDonateCfg();
        if (playerData == null || alliance == null || playerData.donateCountOf(today) != 0
                || cfg == null || cfg.getMemberDailyLimit() <= 0 || cfg.getCounts() == null
                || cfg.getCounts().isEmpty()) {
            return false;
        }
        Long firstCost = cfg.getCounts().get(0);
        return firstCost != null && firstCost <= 0;
    }

    private ApplicationSnapshot applicationSnapshot(AllianceData alliance, long playerId, long now) {
        if (alliance == null || !alliance.isLeader(playerId) || alliance.getApplications().isEmpty()) {
            return ApplicationSnapshot.EMPTY;
        }
        int count = 0;
        long nextExpireTime = 0;
        for (Map.Entry<Long, AllianceApplication> entry : alliance.getApplications().entrySet()) {
            AllianceApplication application = entry.getValue();
            if (application == null
                    || now - application.getApplyTime() > AllianceConst.Cfg.APPLICATION_VALID_MILLS) {
                continue;
            }
            count++;
            long expireTime = application.getApplyTime() + AllianceConst.Cfg.APPLICATION_VALID_MILLS + 1;
            if (nextExpireTime == 0 || expireTime < nextExpireTime) {
                nextExpireTime = expireTime;
            }
        }
        return new ApplicationSnapshot(count, nextExpireTime);
    }

    private RedDotDetails buildDetails(int submodule, int count, RedDotDetails.RedDotType type) {
        RedDotDetails details = new RedDotDetails();
        details.setRedDotModule(getModule());
        details.setRedDotSubmodule(submodule);
        details.setRedDotType(type);
        details.setCount(type == RedDotDetails.RedDotType.COMMON && count > 0 ? 1 : Math.max(0, count));
        return details;
    }

    private void updateDonateDay(long playerId, int today) {
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        if (ctx != null) {
            ctx.setAllianceDonateRedDotDay(today);
        }
    }

    private void updateApplicationExpireTime(long playerId, long nextExpireTime) {
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        if (ctx != null) {
            ctx.setAllianceApplicationRedDotNextExpireTime(nextExpireTime);
        }
    }

    private void updateTaskDay(long playerId, int today) {
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        if (ctx != null) {
            ctx.setAllianceTaskRedDotDay(today);
        }
    }

    private record ApplicationSnapshot(int count, long nextExpireTime) {
        private static final ApplicationSnapshot EMPTY = new ApplicationSnapshot(0, 0);
    }
}
