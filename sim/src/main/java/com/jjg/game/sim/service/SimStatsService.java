package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.constant.SimStatKey;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimCasinoUnlock;
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
import java.util.HashSet;
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
     * 记录一次 slots 旋转的统计 (运行在玩家线程; 直接操作当前场景内存数据)
     */
    public void recordSpin(SimBaseData baseData, int gameType, SpinStatInfo info) {
        if (baseData == null || info == null) {
            return;
        }
        SlotGameStatsData s = baseData.findOrCreateSlotStats(gameType);
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
     * 经营信息-运营数据 (实时刷新当前场景数据)
     */
    public void onOperationData(SimPlayerContext ctx) {
        ResOperationData res = new ResOperationData(Code.SUCCESS);
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("获取运营数据失败, 当前场景为空 playerId={}", ctx.playerId());
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
     * 经营信息-SPINE游戏数据 (>0指定游戏, 0所有游戏汇总)
     */
    public void onSlotStat(SimPlayerContext ctx, int gameType) {
        ResSlotStat res = new ResSlotStat(Code.SUCCESS);
        res.gameType = gameType;
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("获取SPINE游戏数据失败, 当前场景为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            res.stats = buildSlotStats(ctx, gameType);
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

        //容纳游客人数: 已解锁建筑当前容纳 / 全部建筑满级容纳
        int curCapacity = buildingService.computeCurrentCapacity(casino);
        int maxCapacity = buildingService.computeMaxCapacity(casino.getCasinoId());
        list.add(new StatInfo(SimStatKey.Operation.CAPACITY, curCapacity, maxCapacity));

        //雇员人数: 已激活 / 总数(雇员配置表条数)
        int activated = ctx.getEmployeeMap() == null ? 0 : ctx.getEmployeeMap().size();
        int totalEmployee = GameDataManager.getEmployeeProfileCfgList() == null ? 0 : GameDataManager.getEmployeeProfileCfgList().size();
        list.add(new StatInfo(SimStatKey.Operation.EMPLOYEE, activated, totalEmployee));

        //玩家跨娱乐城累计统计
        SimBaseData baseData = ctx.getSimBaseData();
        list.add(new StatInfo(SimStatKey.Operation.RECEPTION, baseData.getReceptionCount()));
        list.add(new StatInfo(SimStatKey.Operation.BUSINESS_INCOME, baseData.getBusinessIncome()));
        list.add(new StatInfo(SimStatKey.Operation.FINISH_TASK, baseData.getFinishedTaskCount()));
        list.add(new StatInfo(SimStatKey.Operation.WATCH_AD, baseData.getWatchAdCount()));

        //房间每分钟产量 (金币收益=SLOT+扑克+捕鱼 三类游戏房间合计)
        Map<Integer, Long> roomOutputs = buildingService.computeRoomOutputs(ctx, casino);
        long slotOutput = roomOutputs.getOrDefault(SimStatKey.Operation.GOLD_INCOME, 0L);
        long pokerOutput = roomOutputs.getOrDefault(SimStatKey.Operation.POKER_ROOM, 0L);
        long fishingOutput = roomOutputs.getOrDefault(SimStatKey.Operation.FISHING_ROOM, 0L);
        list.add(new StatInfo(SimStatKey.Operation.ENERGY_ROOM, roomOutputs.getOrDefault(SimStatKey.Operation.ENERGY_ROOM, 0L)));
        list.add(new StatInfo(SimStatKey.Operation.GOLD_INCOME, slotOutput + pokerOutput + fishingOutput));
        list.add(new StatInfo(SimStatKey.Operation.POKER_ROOM, pokerOutput));
        list.add(new StatInfo(SimStatKey.Operation.FISHING_ROOM, fishingOutput));

        //职能部门当前等级属性值
        list.add(new StatInfo(SimStatKey.Operation.RECEPTION_AREA, buildingService.computeDeptValue(ctx, casino, BuildingOutputType.SERVICE)));
        list.add(new StatInfo(SimStatKey.Operation.MARKETING_DEPT, buildingService.computeDeptValue(ctx, casino, BuildingOutputType.EXPOSURE)));
        list.add(new StatInfo(SimStatKey.Operation.OPERATIONS_DEPT, buildingService.computeDeptValue(ctx, casino, BuildingOutputType.AWARENESS)));

        //研发部: 已研发 / 游戏总数(不受研究院等级影响)
        Set<Integer> unlockGames = findAllUnlockedGames(ctx);
        list.add(new StatInfo(SimStatKey.Operation.UNLOCK_GAME, unlockGames.size()));
        int researched = countResearchedGames(ctx, unlockGames);
        list.add(new StatInfo(SimStatKey.Operation.RESEARCH_DEPT, researched, countConfiguredGames(ctx)));

        return list;
    }

    /**
     * 组装 SPINE游戏数据列表 (gameType<=0 时汇总所有游戏)
     */
    private List<StatInfo> buildSlotStats(SimPlayerContext ctx, int gameType) {
        List<StatInfo> list = new ArrayList<>();

        //玩家所有已解锁娱乐城的游戏并集
        Set<Integer> unlockGames = findAllUnlockedGames(ctx);
        int unlockCount = unlockGames.size();
        list.add(new StatInfo(SimStatKey.Slot.UNLOCK_GAME, unlockCount));

        SlotGameStatsData s = aggregateSlotStats(ctx.getSimBaseData().getSlotStatsMap(), gameType);
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
     * gameType>0 返回指定游戏快照; gameType<=0 汇总所有游戏。
     */
    static SlotGameStatsData aggregateSlotStats(Map<Integer, SlotGameStatsData> statsMap, int gameType) {
        SlotGameStatsData result = new SlotGameStatsData();
        if (statsMap == null || statsMap.isEmpty()) {
            return result;
        }
        if (gameType > 0) {
            result.mergeFrom(statsMap.get(gameType));
            return result;
        }
        for (SlotGameStatsData stats : statsMap.values()) {
            result.mergeFrom(stats);
        }
        return result;
    }

    /**
     * 玩家已解锁的所有游戏并集 (语义同大厅游戏列表 HallService.getSortGameList):
     * 任一已解锁场景的研究院等级达到 ResearchInstitute 配置的等级即解锁。
     * 解锁数据取 ctx 登录缓存, 不在高频看板路径上同步读 Redis。
     */
    private Set<Integer> findAllUnlockedGames(SimPlayerContext ctx) {
        Set<Integer> result = new HashSet<>();
        SimCasinoUnlock unlock = ctx.getCasinoUnlock();
        if (unlock == null || unlock.getResearchLevelMap() == null) {
            return result;
        }
        for (Map.Entry<Integer, Integer> en : unlock.getResearchLevelMap().entrySet()) {
            Set<Integer> games = configCache.getUnlockGameByRegionId(en.getKey());
            if (games == null) {
                continue;
            }
            for (Integer gameType : games) {
                Integer needLevel = configCache.getUnlockGameLevel(en.getKey(), gameType);
                if (needLevel != null && en.getValue() >= needLevel) {
                    result.add(gameType);
                }
            }
        }
        return result;
    }

    /**
     * 玩家已解锁娱乐城配置的全部游戏数 (研发部进度分母, 不受研究院等级影响)
     */
    private int countConfiguredGames(SimPlayerContext ctx) {
        SimCasinoUnlock unlock = ctx.getCasinoUnlock();
        if (unlock == null || unlock.getResearchLevelMap() == null) {
            return 0;
        }
        Set<Integer> result = new HashSet<>();
        for (Integer casinoId : unlock.getResearchLevelMap().keySet()) {
            Set<Integer> games = configCache.getUnlockGameByRegionId(casinoId);
            if (games != null) {
                result.addAll(games);
            }
        }
        return result.size();
    }

    /**
     * 统计当前场景已研发的游戏数 (已拥有技能数据视为已研发)
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
