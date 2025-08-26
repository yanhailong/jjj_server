package com.jjg.game.hall.casino.pb;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.data.Item;
import com.jjg.game.hall.casino.data.CasinoEmployment;
import com.jjg.game.hall.casino.data.CasinoInfo;
import com.jjg.game.hall.casino.data.CasinoMachineInfo;
import com.jjg.game.hall.casino.data.TimeNodeData;
import com.jjg.game.hall.casino.pb.bean.CasinoFloorInfo;
import com.jjg.game.hall.casino.pb.bean.CasinoMachineShowInfo;
import com.jjg.game.hall.casino.pb.bean.CasinoSimpleInfo;
import com.jjg.game.hall.pb.struct.ItemInfo;
import com.jjg.game.hall.utils.GlobalDataCache;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingFunctionCfg;
import com.jjg.game.sampledata.bean.BuildingGainCfg;
import com.jjg.game.sampledata.bean.DealerFunctionCfg;

import java.util.*;

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

    public static CasinoFloorInfo buildCasinoFloorInfo(CasinoInfo casinoInfo, int floorId, long timeMillis, boolean detail) {
        CasinoFloorInfo casinoFloorInfo = new CasinoFloorInfo();
        casinoFloorInfo.casinoMachineShowInfos = new ArrayList<>();
        List<Long> machineIdList = casinoInfo.getBuildingData().get(floorId);
        //构建机台信息
        if (Objects.nonNull(machineIdList) && detail) {
            Map<Integer, Integer> casinoMaxProfitBonus = getCasinoMaxProfitBonus(casinoInfo, timeMillis);
            Map<Long, CasinoMachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
            for (Long machineId : machineIdList) {
                CasinoMachineInfo machineInfo = machineInfoData.get(machineId);
                CasinoMachineShowInfo casinoMachineShowInfo = CasinoBuilder.buildCasinoMachineInfo(machineInfo, casinoMaxProfitBonus, timeMillis);
                casinoFloorInfo.casinoMachineShowInfos.add(casinoMachineShowInfo);
            }
        }
        long cleanEndTime = casinoInfo.getBuildingCleaningEndTime().getOrDefault(floorId, 0L);
        casinoFloorInfo.floorId = floorId;
        casinoFloorInfo.state = getFloorState(cleanEndTime, timeMillis);
        casinoFloorInfo.cleaningEndTime = cleanEndTime;
        return casinoFloorInfo;
    }

    /**
     * 获取我的赌场最大收益加成（排除雇佣）
     *
     * @param casinoInfo 赌场信息
     * @param timeMillis 时间戳
     * @return 加成map
     */
    public static Map<Integer, Integer> getCasinoMaxProfitBonus(CasinoInfo casinoInfo, long timeMillis) {
        Map<Integer, Integer> add = new HashMap<>();
        for (CasinoMachineInfo machineInfo : casinoInfo.getMachineInfoData().values()) {
            int realConfigId = machineInfo.getRealConfigId(timeMillis);
            BuildingFunctionCfg functionCfg = GameDataManager.getBuildingFunctionCfg(realConfigId);
            if (Objects.isNull(functionCfg)) {
                continue;
            }
            if (functionCfg.getTypeID() > 0) {
                BuildingGainCfg buildingGainCfg = GameDataManager.getBuildingGainCfg(functionCfg.getBuffid());
                if (Objects.isNull(buildingGainCfg)) {
                    continue;
                }
                add.merge(buildingGainCfg.getBufftype(), buildingGainCfg.getAddvalue(), Integer::sum);
            }
        }
        return add;
    }


    public static CasinoMachineShowInfo buildCasinoMachineInfo(CasinoMachineInfo machineInfo, Map<Integer, Integer> casinoMaxProfitBonus, long timeMillis) {
        CasinoMachineShowInfo casinoMachineShowInfo = new CasinoMachineShowInfo();
        casinoMachineShowInfo.machineId = machineInfo.getId();
        casinoMachineShowInfo.buildLvUpEndTime = machineInfo.getBuildLvUpEndTime();
        casinoMachineShowInfo.configId = machineInfo.getRealConfigId(timeMillis);
        casinoMachineShowInfo.profitStartTime = machineInfo.getProfitStartTime();
        casinoMachineShowInfo.state = getMachineState(machineInfo, timeMillis);
        casinoMachineShowInfo.profitMaxTime = calculateMaxProfitTime(machineInfo, casinoMaxProfitBonus, timeMillis);
        if (CollectionUtil.isNotEmpty(machineInfo.getEmploymentMap())) {
            casinoMachineShowInfo.employments = new ArrayList<>();
            for (CasinoEmployment employment : machineInfo.getEmploymentMap().values()) {
                casinoMachineShowInfo.employments.add(employment.buildNewCasinoEmploymentInfo());
            }
        }
        return casinoMachineShowInfo;
    }

    public static CasinoSimpleInfo buildCasinoSimpleMachineInfo(CasinoInfo casinoInfo, CasinoMachineInfo casinoMachineInfo, long timeMillis) {
        CasinoSimpleInfo casinoSimpleInfo = new CasinoSimpleInfo();
        Map<Integer, Integer> casinoMaxProfitBonus = getCasinoMaxProfitBonus(casinoInfo, timeMillis);
        casinoSimpleInfo.machineId = casinoMachineInfo.getId();
        casinoSimpleInfo.configId = casinoMachineInfo.getRealConfigId(timeMillis);
        casinoSimpleInfo.profitStartTime = casinoMachineInfo.getProfitStartTime();
        casinoSimpleInfo.profitMaxTime = calculateMaxProfitTime(casinoMachineInfo, casinoMaxProfitBonus, timeMillis);
        return casinoSimpleInfo;
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

    public static int getFloorState(long cleanEndTime, long timeMillis) {
        if (cleanEndTime == 0 || cleanEndTime > timeMillis) {
            //待清理
            return 2;
        }
        //运行中
        return 3;
    }

    public static ItemInfo calculateCostItemInfo(long endTime, long timeMillis) {
        long needTime = endTime - timeMillis;
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

    public static int getMachineState(CasinoMachineInfo casinoMachineInfo, long timeMillis) {
        BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
        if (cfg.getBuldlevel() == 0) {
            //未装备
            return 1;
        }
        return 0;
    }

    public static int getTotalNum(Map<Integer, Integer> casinoMaxProfitBonus, int base) {
        Integer add = casinoMaxProfitBonus.getOrDefault(2, 0);
        base += add;
        Integer addRatio = casinoMaxProfitBonus.getOrDefault(12, 0);
        base = base * (10000 + addRatio) / 10000;
        return base;
    }

    public static int getInstantaneousNum(Map<Integer, Integer> casinoMaxProfitBonus, int base) {
        Integer add = casinoMaxProfitBonus.getOrDefault(1, 0);
        base += add;
        Integer addRatio = casinoMaxProfitBonus.getOrDefault(11, 0);
        base = base * (10000 + addRatio) / 10000;
        return base;
    }

    public static long calculateMaxProfitTime(CasinoMachineInfo casinoMachineInfo, Map<Integer, Integer> casinoMaxProfitBonus, long timeMillis) {
        BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
        if (casinoMachineInfo.getRunEmploymentNum(timeMillis) < cfg.getNumEmployees()) {
            return 0;
        }
        if (cfg.getSavenum() == 0) {
            return 0;
        }
        Map<Integer, Integer> hashMap = new HashMap<>(casinoMaxProfitBonus);
        //添加雇佣的收益
        if (CollectionUtil.isNotEmpty(casinoMachineInfo.getEmploymentMap())) {
            for (CasinoEmployment employment : casinoMachineInfo.getEmploymentMap().values()) {
                if (employment.getEmploymentEndTime() > timeMillis) {
                    DealerFunctionCfg dealerFunctionCfg = GameDataManager.getDealerFunctionCfg(employment.getEmploymentId());
                    if (Objects.isNull(dealerFunctionCfg)) {
                        continue;
                    }
                    BuildingGainCfg buildingGainCfg = GameDataManager.getBuildingGainCfg(dealerFunctionCfg.getBuffid());
                    if (Objects.isNull(buildingGainCfg)) {
                        continue;
                    }
                    hashMap.merge(buildingGainCfg.getBufftype(), buildingGainCfg.getAddvalue(), Integer::sum);
                }
            }
        }
        //计算瞬时收益
        //时间
        int intervalTime = cfg.getOutput().getFirst();
        //数量
        int num = getInstantaneousNum(hashMap, cfg.getOutput().getLast());
        //总数量
        long totalNum = getTotalNum(hashMap, cfg.getSavenum());
        totalNum = totalNum - casinoMachineInfo.getLastProfit();
        if (totalNum <= 0) {
            return 0;
        }
        long remainder = totalNum % num;
        long times = ((totalNum - remainder) / num) + (remainder > 0 ? 1 : 0);
        return times * intervalTime * ONE_MINUTE_OF_MILLIS + casinoMachineInfo.getProfitStartTime();
    }


    /**
     * 获取机台总收益
     *
     * @param areaAdd
     * @param casinoMachineInfo 机台信息
     * @param cfg               配置信息
     * @param timeMillis        当前时间戳
     * @return 机台总收益
     */
    public static long getTotalNum(List<TimeNodeData> areaAdd, CasinoMachineInfo casinoMachineInfo, BuildingFunctionCfg cfg, long timeMillis) {
        long totalNum = casinoMachineInfo.getLastProfit();
        long startTime = casinoMachineInfo.getProfitStartTime();
        //获取雇员数据
        Map<Integer, CasinoEmployment> employmentMap = casinoMachineInfo.getEmploymentMap();
        List<TimeNodeData> tempAreaAdd = new ArrayList<>(areaAdd);
        //计算雇员在的时候的总收益加成
        if (CollectionUtil.isNotEmpty(employmentMap)) {
            if (cfg.getNumEmployees() > employmentMap.size()) {
                return 0;
            }
            for (CasinoEmployment employment : employmentMap.values()) {
                if (employment.getEmploymentEndTime() < startTime) {
                    continue;
                }
                tempAreaAdd.add(TimeNodeData.getNewTimeNodeData(employment));
            }
        }
        if (tempAreaAdd.size() - areaAdd.size() < cfg.getNumEmployees()) {
            return 0;
        }
        //按时间拆分的时间段
        List<Long> timePeriod = new ArrayList<>();
        timePeriod.add(startTime);
        timePeriod.add(timeMillis);
        //全部在时间内的
        List<TimeNodeData> allInTime = new ArrayList<>();
        //去除全在时间内的
        Iterator<TimeNodeData> iterator = tempAreaAdd.iterator();
        while (iterator.hasNext()) {
            TimeNodeData next = iterator.next();
            if (next.getStartTime() < startTime && next.getEndTime() < startTime) {
                allInTime.add(next);
                iterator.remove();
                continue;
            }
            timePeriod.add(Math.max(next.getStartTime(), startTime));
            timePeriod.add(Math.min(timeMillis, next.getEndTime()));
        }
        Map<Integer, Integer> base = new HashMap<>();
        if (!allInTime.isEmpty()) {
            for (TimeNodeData timeNodeData : allInTime) {
                //机台
                if (timeNodeData.getType() == 1) {
                    BuildingFunctionCfg buildingFunctionCfg = GameDataManager.getBuildingFunctionCfg(timeNodeData.getLastLevelConfigId());
                    BuildingGainCfg buildingGainCfg = GameDataManager.getBuildingGainCfg(buildingFunctionCfg.getBuffid());
                    base.merge(buildingGainCfg.getBufftype(), buildingGainCfg.getAddvalue(), Integer::sum);
                } else {
                    DealerFunctionCfg dealerFunctionCfg = GameDataManager.getDealerFunctionCfg(timeNodeData.getConfigId());
                    BuildingGainCfg buildingGainCfg = GameDataManager.getBuildingGainCfg(dealerFunctionCfg.getBuffid());
                    base.merge(buildingGainCfg.getBufftype(), buildingGainCfg.getAddvalue(), Integer::sum);
                }
            }
        }
        //计算时间 按时间排序
        List<Long> finalTimePeriod = timePeriod.stream().distinct().sorted().toList();
        //收益结束到现在全部都在时间内
        for (int i = 0; i < finalTimePeriod.size(); i++) {
            Long startPeriod = finalTimePeriod.get(i);
            int nextTime = i + 1;
            Map<Integer, Integer> casinoMaxProfitBonus;
            Long endPeriod = timeMillis;
            if (nextTime < finalTimePeriod.size()) {
                endPeriod = finalTimePeriod.get(nextTime);
            } else {
                //直接计算单个
                if (startPeriod.equals(endPeriod)) {
                    break;
                }
            }
            casinoMaxProfitBonus = getCasinoMaxProfitBonus(tempAreaAdd, base, startPeriod, endPeriod);
            //总数量
            int totalMaxNum = getTotalNum(casinoMaxProfitBonus, cfg.getSavenum());
            if (totalNum >= totalMaxNum) {
                continue;
            }
            //时间
            int intervalTime = cfg.getOutput().getFirst() * ONE_MINUTE_OF_MILLIS;
            long period = endPeriod - startTime;
            long times = period / intervalTime;
            if (times == 0) {
                continue;
            }
            //数量
            int num = getInstantaneousNum(casinoMaxProfitBonus, cfg.getOutput().getLast());
            long remainder = period % intervalTime;
            totalNum = Math.min(totalNum + times * num + (remainder > 0 ? num : 0), totalMaxNum);
        }
        return totalNum;
    }

    public static Map<Integer, Integer> getCasinoMaxProfitBonus(List<TimeNodeData> data, Map<Integer, Integer> base, long startTime, long endTime) {
        Map<Integer, Integer> addMap = new HashMap<>(base);
        for (TimeNodeData nodeData : data) {
            if (nodeData.getType() == 2) {
                //雇员
                if (nodeData.getStartTime() <= startTime && nodeData.getEndTime() >= endTime) {
                    //包含
                    DealerFunctionCfg cfg = GameDataManager.getDealerFunctionCfg(nodeData.getConfigId());
                    BuildingGainCfg buildingGainCfg = GameDataManager.getBuildingGainCfg(cfg.getBuffid());
                    addMap.merge(buildingGainCfg.getBufftype(), buildingGainCfg.getAddvalue(), Integer::sum);
                }
            } else {
                //机台
                int id = nodeData.getLastLevelConfigId();
                if (nodeData.getEndTime() < endTime) {
                    id = nodeData.getConfigId();
                }
                BuildingFunctionCfg buildingFunctionCfg = GameDataManager.getBuildingFunctionCfg(id);
                BuildingGainCfg buildingGainCfg = GameDataManager.getBuildingGainCfg(buildingFunctionCfg.getBuffid());
                addMap.merge(buildingGainCfg.getBufftype(), buildingGainCfg.getAddvalue(), Integer::sum);
            }
        }
        return addMap;
    }

    public static List<TimeNodeData> getTimeNodeData(Map<Long, CasinoMachineInfo> machineInfoData, long timeMillis) {
        //总加成
        List<TimeNodeData> areaAdd = new ArrayList<>();
        //获取提款机等级 获取休息区等级
        for (CasinoMachineInfo casinoMachineInfo : machineInfoData.values()) {
            BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
            if (Objects.isNull(cfg)) {
                continue;
            }
            if (cfg.getBuffid() > 0) {
                BuildingGainCfg buildingGainCfg = GameDataManager.getBuildingGainCfg(cfg.getBuffid());
                if (Objects.isNull(buildingGainCfg) || buildingGainCfg.getBufftype() == 0) {
                    continue;
                }
                BuildingFunctionCfg functionCfg = CasinoBuilder.getLastBuildingFunctionCfg(buildingGainCfg.getId());
                areaAdd.add(TimeNodeData.getNewTimeNodeData(casinoMachineInfo, Objects.isNull(functionCfg) ? 0 : functionCfg.getId()));
            }
        }
        return areaAdd;
    }
}
