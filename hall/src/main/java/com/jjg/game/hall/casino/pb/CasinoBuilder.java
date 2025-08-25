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
import static com.jjg.game.hall.constant.HallConstant.Casino.REST_AREA_TYPE;
import static com.jjg.game.hall.constant.HallConstant.Casino.WITHDRAWAL_AREA_TYPE;


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
            if (functionCfg.getTypeID() == REST_AREA_TYPE || functionCfg.getTypeID() == WITHDRAWAL_AREA_TYPE) {
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
                casinoMachineShowInfo.employments.add(employment.getNewCasinoEmploymentInfo());
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
        Integer intervalTime = cfg.getOutput().getFirst();
        //数量
        int num = getInstantaneousNum(hashMap, cfg.getOutput().getLast());
        //总数量
        int totalNum = getTotalNum(hashMap, cfg.getSavenum());
        int remainder = totalNum % num;
        long times = ((totalNum - remainder) / num) + remainder > 0 ? 1 : 0;
        return times * intervalTime * ONE_MINUTE_OF_MILLIS;
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
        long totalNum = 0;
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
        //去除全在时间内的
        Iterator<TimeNodeData> iterator = areaAdd.iterator();
        List<TimeNodeData> allInTime = new ArrayList<>();
        while (iterator.hasNext()) {
            TimeNodeData next = iterator.next();
            if (next.getStartTime() < startTime && next.getEndTime() < startTime) {
                allInTime.add(next);
                iterator.remove();
            }
        }
        //计算时间 按时间排序
        areaAdd.sort(Comparator.comparingLong(TimeNodeData::getStartTime));
        //收益结束到现在全部都在时间内
        for (int i = 0; i < areaAdd.size(); i++) {
            TimeNodeData info = areaAdd.get(i);
            //从时间线计算每段时间的收益
            if (info.getStartTime() < startTime && info.getEndTime() > startTime) {

            }
            //2.结束时间在内
            //3.开始时间在内
        }
        long remain = cfg.getSavenum() - totalNum;
        if (remain <= 0) {
            return totalNum;
        }
        //计算升级结束到现在时间的收益
        totalNum += getProfitMaxNum(cfg, startTime, timeMillis);
        return Math.min(totalNum, cfg.getSavenum());
    }

    public static Map<Integer, Integer> getCasinoMaxProfitBonus(List<TimeNodeData> data, Map<Integer, Integer> base, long endTime) {
        return null;
    }
}
