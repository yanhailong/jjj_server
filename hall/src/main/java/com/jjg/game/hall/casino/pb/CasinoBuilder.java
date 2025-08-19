package com.jjg.game.hall.casino.pb;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.data.Item;
import com.jjg.game.hall.casino.data.CasinoEmployment;
import com.jjg.game.hall.casino.data.MachineInfo;
import com.jjg.game.hall.casino.pb.bean.CasinoEmploymentInfo;
import com.jjg.game.hall.casino.pb.bean.CasinoMachineInfo;
import com.jjg.game.hall.pb.struct.ItemInfo;
import com.jjg.game.hall.utils.GlobalDataCache;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingFunctionCfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.jjg.game.common.utils.TimeHelper.ONE_MINUTE_OF_MILLIS;


/**
 * @author lm
 * @date 2025/8/19 10:08
 */
public class CasinoBuilder {
    public static ItemInfo buildItemInfo(Item item) {
        ItemInfo itemInfo = new ItemInfo();
        itemInfo.itemId = item.getId();
        itemInfo.count = item.getCount();
        return itemInfo;
    }

    public static CasinoMachineInfo buildCasinoMachineInfo(MachineInfo machineInfo, long timeMillis) {
        CasinoMachineInfo casinoMachineInfo = new CasinoMachineInfo();
        casinoMachineInfo.machineId = machineInfo.getId();
        casinoMachineInfo.buildLvUpEndTime = machineInfo.getBuildLvUpEndTime();
        casinoMachineInfo.configId = machineInfo.getConfigId();
        casinoMachineInfo.profitStartTime = machineInfo.getProfitStartTime();
        casinoMachineInfo.state = getMachineState(machineInfo);
        casinoMachineInfo.itemInfo = calculateCostItemInfo(machineInfo, timeMillis);
        casinoMachineInfo.profitMaxTime = calculateMaxProfitTime(machineInfo, timeMillis);
        if (Objects.nonNull(machineInfo.getEmploymentList())) {
            casinoMachineInfo.employments = new ArrayList<>();
            for (CasinoEmployment employment : machineInfo.getEmploymentList()) {
                casinoMachineInfo.employments.add(employment.getCasinoEmploymentInfo());
            }
        }
        return casinoMachineInfo;
    }

    public static long getProfitMaxNum(BuildingFunctionCfg cfg, long startTime, long endTime) {
        //计算升级前的收益
        //计算触发次数
        long times = Math.max(0, endTime - startTime) / (ONE_MINUTE_OF_MILLIS);
        //每次获取数量
        Integer everyTimeCount = cfg.getOutput().getLast();
        //总获取
        long totalCount = times * everyTimeCount;
        return Math.min(cfg.getSavenum(), totalCount);
    }

    public static BuildingFunctionCfg getLastBuildingFunctionCfg(int configId) {
        List<BuildingFunctionCfg> buildingFunctionCfgList = GameDataManager.getBuildingFunctionCfgList();
        for (BuildingFunctionCfg functionCfg : buildingFunctionCfgList) {
            if (functionCfg.getNextlevelID() == configId) {
                return functionCfg;
            }
        }
        return null;
    }

    public static ItemInfo calculateCostItemInfo(MachineInfo machineInfo, long timeMillis) {
        long needTime = machineInfo.getBuildLvUpEndTime() - timeMillis;
        if (needTime < 0) {
            return null;
        }
        Pair<Item, Integer> reduceTimeConfig = GlobalDataCache.getReduceTimeConfig();
        //需要的秒数
        long needS = needTime / 1000;
        long num = needS / reduceTimeConfig.getSecond() + (needS % reduceTimeConfig.getSecond() == 0 ? 0 : 1);
        ItemInfo itemInfo = new ItemInfo();
        itemInfo.itemId = reduceTimeConfig.getFirst().getId();
        itemInfo.count = reduceTimeConfig.getFirst().getCount() * num;
        return itemInfo;
    }

    public static int getMachineState(MachineInfo machineInfo) {
        BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(machineInfo.getConfigId());
        if (cfg.getBuldlevel() == 0) {
            //未装备
            return 1;
        }
        return 0;
    }

    public static long calculateMaxProfitTime(MachineInfo machineInfo, long timeMillis) {
        //TODO 需要雇员
        long totalNum = 0;
        BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(machineInfo.getConfigId());
        long startTime = machineInfo.getProfitStartTime();
        //在收益阶段升级
        if (machineInfo.getBuildLvUpStartTime() > machineInfo.getProfitStartTime()) {
            BuildingFunctionCfg lastBuildingFunctionCfg = getLastBuildingFunctionCfg(cfg.getId());
            if (Objects.nonNull(lastBuildingFunctionCfg)) {
                //计算升级前的收益
                totalNum = getProfitMaxNum(lastBuildingFunctionCfg, machineInfo.getBuildLvUpStartTime(), machineInfo.getProfitStartTime());
                startTime = machineInfo.getBuildLvUpEndTime();
            }
        }
        long remain = cfg.getSavenum() - totalNum;
        if (remain <= 0) {
            return -1;
        }
        //计算升级结束到现在时间的收益
        totalNum += getProfitMaxNum(cfg, startTime, timeMillis);
        remain = cfg.getSavenum() - totalNum;
        if (remain <= 0) {
            return -1;
        }
        //计算需要的时间
        boolean remainder = remain % cfg.getOutput().getLast() == 0;
        return remain / cfg.getOutput().getLast() * ONE_MINUTE_OF_MILLIS + (remainder ? 0 : 1) + timeMillis;
    }
}
