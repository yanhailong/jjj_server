package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoListCfg;
import com.jjg.game.sim.constant.BonusType;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.constant.SimStatKey;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.data.SlotGameStatsData;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.pb.res.ResOperationData;
import com.jjg.game.sim.pb.res.ResSlotStat;
import com.jjg.game.sim.pb.struct.StatInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 经营信息看板: SPINE游戏旋转统计累加 + 运营/SPINE数据读取下发。
 *
 * @author 11
 * @date 2026/6/15
 */
@Service
public class SimStatsService {
    private static final Logger log = LoggerFactory.getLogger(SimStatsService.class);

    @Autowired
    private SimBuildingService buildingService;
    @Autowired
    private SimConfigCacheService configCache;

    // ---------------------------------------------------------------------
    // 累加 (slots 旋转上报)
    // ---------------------------------------------------------------------

    /**
     * 记录一次 slots 旋转的统计 (运行在玩家线程; 直接操作当前赌场内存数据)
     */
    public void recordSpin(SimCasinoData casino, int gameType, SpinStatInfo info) {
        if (casino == null || info == null) {
            return;
        }
        SlotGameStatsData s = casino.findOrCreateSlotStats(gameType);
        if (info.getBet() > 0) {
            s.setTotalBet(s.getTotalBet() + info.getBet());
        }
        s.setSpinCount(s.getSpinCount() + 1);
        if (info.getWin() > 0) {
            s.setTotalWin(s.getTotalWin() + info.getWin());
            if (info.getWin() > s.getMaxWin()) {
                s.setMaxWin(info.getWin());
            }
        }
        if (info.getMultiple() > s.getMaxMultiple()) {
            s.setMaxMultiple(info.getMultiple());
        }
        //大奖次数
        switch (info.getBigShowId()) {
            case SimConstant.BigWinShow.SWEET -> s.setSweetWin(s.getSweetWin() + 1);
            case SimConstant.BigWinShow.BIG -> s.setBigWin(s.getBigWin() + 1);
            case SimConstant.BigWinShow.MEGA -> s.setMegaWin(s.getMegaWin() + 1);
            case SimConstant.BigWinShow.EPIC -> s.setEpicWin(s.getEpicWin() + 1);
            case SimConstant.BigWinShow.LEGENDARY -> s.setLegendaryWin(s.getLegendaryWin() + 1);
            default -> {
            }
        }
        //奖池次数
        if (info.getMini() > 0) {
            s.setMiniCount(s.getMiniCount() + 1);
        }
        if (info.getMinor() > 0) {
            s.setMinorCount(s.getMinorCount() + 1);
        }
        if (info.getMajor() > 0) {
            s.setMajorCount(s.getMajorCount() + 1);
        }
        if (info.getGrand() > 0) {
            s.setGrandCount(s.getGrandCount() + 1);
        }
        //免费游戏触发: 剩余免费次数 0 -> >0 视为一次触发
        if (s.getLastRemainFree() == 0 && info.getRemainFreeCount() > 0) {
            s.setFreeCount(s.getFreeCount() + 1);
        }
        s.setLastRemainFree(info.getRemainFreeCount());
    }

    // ---------------------------------------------------------------------
    // 读取下发
    // ---------------------------------------------------------------------

    /**
     * 经营信息-运营数据 (实时刷新当前赌场数据)
     */
    public void onOperationData(SimPlayerContext ctx) {
        ResOperationData res = new ResOperationData(Code.SUCCESS);
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("获取运营数据失败, 当前赌场为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            res.stats = buildOperationStats(ctx, casino);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 经营信息-SPINE游戏数据 (指定游戏)
     */
    public void onSlotStat(SimPlayerContext ctx, int gameType) {
        ResSlotStat res = new ResSlotStat(Code.SUCCESS);
        res.gameType = gameType;
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("获取SPINE游戏数据失败, 当前赌场为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            res.stats = buildSlotStats(casino, gameType);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 组装运营数据列表
     */
    private List<StatInfo> buildOperationStats(SimPlayerContext ctx, SimCasinoData casino) {
        List<StatInfo> list = new ArrayList<>();

        //容纳游客人数: 当前 / 最大(娱乐城配置)
        int curCapacity = buildingService.computeCurrentCapacity(casino);
        CasinoListCfg listCfg = GameDataManager.getCasinoListCfg(casino.getCasinoId());
        int maxCapacity = listCfg == null ? 0 : listCfg.getCapacityNum();
        list.add(new StatInfo(SimStatKey.Operation.CAPACITY, curCapacity, maxCapacity));

        //雇员人数: 已激活 / 总数(雇员配置表条数)
        int activated = ctx.getEmployeeMap() == null ? 0 : ctx.getEmployeeMap().size();
        int totalEmployee = GameDataManager.getEmployeeProfileCfgList() == null ? 0 : GameDataManager.getEmployeeProfileCfgList().size();
        list.add(new StatInfo(SimStatKey.Operation.EMPLOYEE, activated, totalEmployee));

        //累计统计
        list.add(new StatInfo(SimStatKey.Operation.RECEPTION, casino.getReceptionCount()));
        list.add(new StatInfo(SimStatKey.Operation.BUSINESS_INCOME, casino.getBusinessIncome()));
        list.add(new StatInfo(SimStatKey.Operation.FINISH_TASK, casino.getFinishedTaskCount()));
        list.add(new StatInfo(SimStatKey.Operation.WATCH_AD, casino.getWatchAdCount()));

        //房间每分钟产量
        Map<Integer, Long> roomOutputs = buildingService.computeRoomOutputs(ctx, casino);
        list.add(new StatInfo(SimStatKey.Operation.ENERGY_ROOM, roomOutputs.getOrDefault(SimStatKey.Operation.ENERGY_ROOM, 0L)));
        list.add(new StatInfo(SimStatKey.Operation.SLOT_ROOM, roomOutputs.getOrDefault(SimStatKey.Operation.SLOT_ROOM, 0L)));
        list.add(new StatInfo(SimStatKey.Operation.POKER_ROOM, roomOutputs.getOrDefault(SimStatKey.Operation.POKER_ROOM, 0L)));
        list.add(new StatInfo(SimStatKey.Operation.FISHING_ROOM, roomOutputs.getOrDefault(SimStatKey.Operation.FISHING_ROOM, 0L)));

        //职能部门当前等级属性值
        list.add(new StatInfo(SimStatKey.Operation.RECEPTION_AREA, buildingService.computeDeptValue(ctx, casino, BuildingOutputType.SERVICE, BonusType.SERVICE)));
        list.add(new StatInfo(SimStatKey.Operation.MARKETING_DEPT, buildingService.computeDeptValue(ctx, casino, BuildingOutputType.EXPOSURE, null)));
        list.add(new StatInfo(SimStatKey.Operation.OPERATIONS_DEPT, buildingService.computeDeptValue(ctx, casino, BuildingOutputType.AWARENESS, BonusType.AWARENESS)));

        //研发部: 已研发 / 游戏总数
        Set<Integer> unlockGames = configCache.getUnlockGameByRegionId(casino.getCasinoId());
        int totalGame = unlockGames == null ? 0 : unlockGames.size();
        int researched = countResearchedGames(ctx, unlockGames);
        list.add(new StatInfo(SimStatKey.Operation.RESEARCH_DEPT, researched, totalGame));

        return list;
    }

    /**
     * 组装 SPINE游戏数据列表 (gameType<=0 时仅返回解锁游戏数)
     */
    private List<StatInfo> buildSlotStats(SimCasinoData casino, int gameType) {
        List<StatInfo> list = new ArrayList<>();

        //解锁游戏数 (当前赌场)
        Set<Integer> unlockGames = configCache.getUnlockGameByRegionId(casino.getCasinoId());
        int unlockCount = unlockGames == null ? 0 : unlockGames.size();
        list.add(new StatInfo(SimStatKey.Slot.UNLOCK_GAME, unlockCount));

        if (gameType <= 0) {
            return list;
        }

        SlotGameStatsData s = casino.findSlotStats(gameType);
        if (s == null) {
            //未游玩过的游戏: 返回全 0
            s = new SlotGameStatsData();
        }
        list.add(new StatInfo(SimStatKey.Slot.TOTAL_BET, s.getTotalBet()));
        list.add(new StatInfo(SimStatKey.Slot.SPIN_COUNT, s.getSpinCount()));
        list.add(new StatInfo(SimStatKey.Slot.TOTAL_WIN, s.getTotalWin()));
        list.add(new StatInfo(SimStatKey.Slot.MAX_WIN, s.getMaxWin()));
        list.add(new StatInfo(SimStatKey.Slot.MAX_MULTIPLE, s.getMaxMultiple()));
        list.add(new StatInfo(SimStatKey.Slot.SWEET_WIN, s.getSweetWin()));
        list.add(new StatInfo(SimStatKey.Slot.BIG_WIN, s.getBigWin()));
        list.add(new StatInfo(SimStatKey.Slot.MEGA_WIN, s.getMegaWin()));
        list.add(new StatInfo(SimStatKey.Slot.EPIC_WIN, s.getEpicWin()));
        list.add(new StatInfo(SimStatKey.Slot.LEGENDARY_WIN, s.getLegendaryWin()));
        list.add(new StatInfo(SimStatKey.Slot.MINI, s.getMiniCount()));
        list.add(new StatInfo(SimStatKey.Slot.MINOR, s.getMinorCount()));
        list.add(new StatInfo(SimStatKey.Slot.MAJOR, s.getMajorCount()));
        list.add(new StatInfo(SimStatKey.Slot.GRAND, s.getGrandCount()));
        list.add(new StatInfo(SimStatKey.Slot.FREE_GAME, s.getFreeCount()));
        return list;
    }

    /**
     * 统计当前赌场已研发的游戏数 (已拥有技能数据视为已研发)
     */
    private int countResearchedGames(SimPlayerContext ctx, Set<Integer> unlockGames) {
        if (unlockGames == null || unlockGames.isEmpty()) {
            return 0;
        }
        Map<Integer, SimSkillsData> skills = ctx.getSkillsDataMap();
        if (skills == null || skills.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Integer game : unlockGames) {
            if (skills.containsKey(game)) {
                count++;
            }
        }
        return count;
    }
}
