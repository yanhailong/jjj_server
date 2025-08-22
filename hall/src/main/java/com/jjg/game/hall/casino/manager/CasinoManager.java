package com.jjg.game.hall.casino.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.hall.casino.data.CasinoEmployment;
import com.jjg.game.hall.casino.data.CasinoInfo;
import com.jjg.game.hall.casino.data.MachineInfo;
import com.jjg.game.hall.casino.pb.bean.CasinoSimpleMachineInfo;
import com.jjg.game.hall.casino.pb.req.*;
import com.jjg.game.hall.casino.pb.res.*;
import com.jjg.game.hall.casino.service.PlayerBuildingService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.hall.casino.data.PlayerBuilding;
import com.jjg.game.hall.casino.pb.CasinoBuilder;
import com.jjg.game.hall.casino.pb.bean.CasinoFloorInfo;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.struct.ItemInfo;
import com.jjg.game.hall.utils.ConditionUtil;
import com.jjg.game.hall.utils.GlobalDataCache;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingFloorCfg;
import com.jjg.game.sampledata.bean.BuildingFunctionCfg;
import com.jjg.game.sampledata.bean.DealerFunctionCfg;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author lm
 * @date 2025/8/18 16:24
 */
@Component
public class CasinoManager {
    private final Logger log = LoggerFactory.getLogger(CasinoManager.class);
    @Autowired
    private PlayerBuildingService playerBuildingService;
    @Autowired
    private PlayerPackService playerPackService;

    /**
     * 请求购买一键领取
     *
     * @param playerId 玩家id
     * @param req      请求
     * @return 响应
     */
    public ResCasinoBuyClaimAllRewards reqCasinoBuyClaimAllRewards(long playerId, ReqCasinoBuyClaimAllRewards req) {
        ResCasinoBuyClaimAllRewards res = new ResCasinoBuyClaimAllRewards();
        res.casinoId = req.casinoId;
        try {
            GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(HallConstant.Casino.BUY_ALL_CLAIM_ALL_REWARDS);
            if (Objects.isNull(globalConfigCfg)) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            String consumeStr = globalConfigCfg.getValue();
            String[] split = StringUtils.split(consumeStr, "_");
            if (split.length != 3) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            //检查消耗
            PlayerPack pack = playerPackService.getFromAllDB(playerId);
            if (Objects.isNull(pack)) {
                res.code = Code.NOT_ENOUGH_ITEM;
                return res;
            }
            Item consume = new Item(Integer.parseInt(split[0]), Long.parseLong(split[1]));
            if (!pack.checkHasItems(List.of(consume))) {
                res.code = Code.NOT_ENOUGH_ITEM;
                return res;
            }
            CommonResult<Long> updated = playerBuildingService.updateData(playerId, req.casinoId, (result, playerBuilding) -> {
                CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
                if (Objects.isNull(casinoInfo)) {
                    result.code = Code.PARAM_ERROR;
                    return;
                }
                long oneClickClaimEndTime = casinoInfo.getOneClickClaimEndTime();
                if (oneClickClaimEndTime > System.currentTimeMillis()) {
                    result.code = Code.PARAM_ERROR;
                    return;
                }
                //扣除道具
                CommonResult<PlayerPack> removed = playerPackService.removeItem(playerId, consume.getId(), consume.getCount(), "一键升级购买");
                if (!removed.success()) {
                    result.code = Code.NOT_ENOUGH_ITEM;
                    return;
                }
                //添加数据
                long endTime = System.currentTimeMillis() + Long.parseLong(split[2]);
                casinoInfo.setOneClickClaimEndTime(endTime);
                result.data = endTime;
                result.code = Code.SUCCESS;
            });
            if (!updated.success()) {
                log.error("购买一键领取时保存结束时间失败 playerId:{}", playerId);
                res.code = updated.code;
                return res;
            }
            res.endTime = updated.data;
            return res;
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求购买一键领取异常", e);
        }
        return res;
    }

    /**
     * 请求我的赌场信息
     *
     * @param playerId 玩家id
     * @param req      请求
     * @return 赌场信息
     */
    public ResCasinoInfo reqCasinoInfo(long playerId, ReqCasinoInfo req) {
        ResCasinoInfo res = new ResCasinoInfo();
        PlayerBuilding playerBuilding = playerBuildingService.getCasinoInfoFromAllDB(playerId, req.casinoId);
        if (Objects.isNull(playerBuilding)) {
            playerBuilding = new PlayerBuilding();
            playerBuilding.setPlayerId(playerId);
            CasinoInfo newCasinoInfo = CasinoInfo.getNewCasinoInfo(req.casinoId);
            playerBuilding.setCasinoInfo(newCasinoInfo);
            newCasinoInfo.getBuildingData().put(0, null);
            newCasinoInfo.getBuildingCleaningEndTime().put(0, System.currentTimeMillis());
            playerBuildingService.redisSave(playerId, req.casinoId, playerBuilding);
//            res.code = Code.UNKNOWN_ERROR;
//            return res;
        }
        CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
        if (Objects.nonNull(casinoInfo)) {
            res.claimAllRewardsEndTime = casinoInfo.getOneClickClaimEndTime();
            Pair<Item, Integer> buyClaimAllRewardsConsumer = GlobalDataCache.getBuyClaimAllRewardsConsumer();
            Item item = buyClaimAllRewardsConsumer.getFirst();
            res.itemInfo = CasinoBuilder.buildItemInfo(item);
            res.casinoId = req.casinoId;
            Map<Integer, List<Long>> map = casinoInfo.getBuildingData();
            res.maxUnlockFloorId = getMaxUnlockFloorId(map);
            //构建全部楼层信息
            res.casinoFloorInfos = new ArrayList<>();
            long timeMillis = System.currentTimeMillis();
            Map<Long, MachineInfo> machineData = casinoInfo.getMachineInfoData();
            for (Map.Entry<Integer, List<Long>> entry : map.entrySet()) {
                //构建楼层信息
                Long endTime = casinoInfo.getBuildingCleaningEndTime().getOrDefault(entry.getKey(), 0L);
                CasinoFloorInfo casinoFloorInfo = CasinoBuilder.buildCasinoFloorInfo(entry.getKey(), entry.getValue(), machineData, endTime, timeMillis);
                res.casinoFloorInfos.add(casinoFloorInfo);
            }
        }
        return res;
    }

    /**
     * 获取最大解锁楼层
     *
     * @return 最大解锁楼层
     */
    public int getMaxUnlockFloorId(Map<Integer, List<Long>> buildingData) {
        return buildingData.keySet()
                .stream()
                .max(Integer::compareTo)
                .orElse(1);
    }

    /**
     * 请求一键领取
     *
     * @param playerId 玩家id
     * @param req      请求
     * @return 一键领取结果
     */
    public ResCasinoClaimRewards reqCasinoClaimAllRewards(long playerId, ReqCasinoClaimAllRewards req) {
        ResCasinoClaimRewards res = new ResCasinoClaimRewards();
        try {
            res.machineId = 0;
            List<CasinoSimpleMachineInfo> casinoSimpleMachineInfos = new ArrayList<>();
            res.casinoSimpleMachineInfos = casinoSimpleMachineInfos;
            CommonResult<Map<Integer, Long>> updated = playerBuildingService.updateData(playerId, req.casinoId, (result, playerBuilding) -> {
                CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
                if (Objects.isNull(casinoInfo)) {
                    result.code = Code.PARAM_ERROR;
                    return;
                }
                Map<Long, MachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
                long timeMillis = System.currentTimeMillis();
                Map<Integer, Long> getReward = new HashMap<>();
                if (CollectionUtil.isNotEmpty(machineInfoData)) {
                    //计算总收获
                    for (MachineInfo machineInfo : machineInfoData.values()) {
                        BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(machineInfo.getConfigId());
                        long totalNum = CasinoBuilder.getTotalNum(machineInfo, cfg, timeMillis);
                        if (totalNum == 0) {
                            continue;
                        }
                        getReward.merge(cfg.getOutput().getFirst(), totalNum, Long::sum);
                        //修改数据
                        machineInfo.setProfitStartTime(timeMillis);
                        casinoSimpleMachineInfos.add(CasinoBuilder.buildCasinoSimpleMachineInfo(machineInfo, timeMillis));
                    }
                    //发奖
                    playerPackService.addItems(playerId, getReward, "一键领取赌场收益");
                }

                result.data = getReward;
                result.code = Code.SUCCESS;
            });
            if (!updated.success()) {
                log.error("一键领取时失败 playerId:{}", playerId);
                res.code = updated.code;
                return res;
            }
            if (CollectionUtil.isNotEmpty(updated.data)) {
                res.itemInfos = new ArrayList<>();
                for (Map.Entry<Integer, Long> entry : updated.data.entrySet()) {
                    ItemInfo itemInfo = new ItemInfo();
                    itemInfo.count = entry.getValue();
                    itemInfo.itemId = entry.getKey();
                    res.itemInfos.add(itemInfo);
                }
            }
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求一键领取机台收益异常", e);
        }
        return res;
    }

    /**
     * 领取机台收益
     *
     * @param playerId 玩家id
     * @param req      请求
     * @return 领取机台收益结果
     */
    public ResCasinoClaimRewards reqCasinoClaimRewards(long playerId, ReqCasinoClaimRewards req) {
        ResCasinoClaimRewards res = new ResCasinoClaimRewards();
        try {
            res.machineId = 0;
            List<CasinoSimpleMachineInfo> casinoSimpleMachineInfos = new ArrayList<>();
            res.casinoSimpleMachineInfos = casinoSimpleMachineInfos;
            CommonResult<Item> updated = playerBuildingService.updateData(playerId, req.casinoId, (result, playerBuilding) -> {
                CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
                if (Objects.isNull(casinoInfo)) {
                    result.code = Code.PARAM_ERROR;
                    return;
                }
                MachineInfo machineInfo = casinoInfo.getMachineInfoData().get(req.machineId);
                if (Objects.isNull(machineInfo)) {
                    result.code = Code.PARAM_ERROR;
                    return;
                }
                BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(machineInfo.getConfigId());
                long timeMillis = System.currentTimeMillis();
                //计算总收获
                long totalNum = CasinoBuilder.getTotalNum(machineInfo, cfg, timeMillis);
                machineInfo.setProfitStartTime(timeMillis);
                //回存
                playerBuildingService.redisSave(playerId, req.casinoId, playerBuilding);
                //发奖
                Item item = new Item(cfg.getOutput().getFirst(), totalNum);
                playerPackService.addItem(playerId, item.getId(), item.getCount(), "一键领取机台收益");
                //TODO
                casinoSimpleMachineInfos.add(CasinoBuilder.buildCasinoSimpleMachineInfo(machineInfo, timeMillis));
                result.data = item;
                result.code = Code.SUCCESS;
            });
            if (!updated.success()) {
                log.error("领取机台收益失败 playerId:{} machineId:{}", playerId, req.machineId);
                res.code = updated.code;
                return res;
            }
            res.itemInfos = new ArrayList<>();
            res.itemInfos.add(CasinoBuilder.buildItemInfo(updated.data));
            return res;
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求领取机台收益异常", e);
        }
        return res;
    }

    /**
     * 请求雇员职员
     *
     * @param player 玩家id
     * @param req    请求
     * @return 响应
     */
    public ResCasinoEmployStaff reqCasinoEmployStaff(long player, ReqCasinoEmployStaff req) {
        ResCasinoEmployStaff res = new ResCasinoEmployStaff();
        DealerFunctionCfg dealerFunctionCfg = GameDataManager.getDealerFunctionCfg(req.staffId);
        if (Objects.isNull(dealerFunctionCfg)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        List<Integer> cost = dealerFunctionCfg.getHiringExpenses();
        Item costItem = new Item(cost.getFirst(), cost.getLast());
        long timeMillis = System.currentTimeMillis();
        try {
            CommonResult<MachineInfo> updated = playerBuildingService.updateData(player, req.casinoId, (result, playerBuilding) -> {
                CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
                if (Objects.isNull(casinoInfo)) {
                    result.code = Code.PARAM_ERROR;
                    return;
                }
                MachineInfo machineInfo = casinoInfo.getMachineInfoData().get(req.machineId);
                if (Objects.isNull(machineInfo)) {
                    result.code = Code.PARAM_ERROR;
                    return;
                }
                Map<Integer, CasinoEmployment> employmentMap = machineInfo.getEmploymentMap();
                CasinoEmployment casinoEmployment = employmentMap.get(req.index);
                //已经购买还没到结束时间
                if (Objects.nonNull(casinoEmployment) && casinoEmployment.getEmploymentEndTime() > timeMillis) {
                    result.code = Code.PARAM_ERROR;
                    return;
                }
                CommonResult<PlayerPack> removed = playerPackService.removeItem(player, costItem, "请求雇员职员");
                if (!removed.success()) {
                    result.code = Code.NOT_ENOUGH_ITEM;
                    return;
                }
                casinoEmployment = new CasinoEmployment();
                employmentMap.put(req.index, casinoEmployment);
                //设置数据
                casinoEmployment.setEmploymentId(req.staffId);
                casinoEmployment.setEmploymentEndTime(timeMillis + dealerFunctionCfg.getDuration());
                result.data = machineInfo;
                result.code = Code.SUCCESS;
            });
            if (!updated.success()) {
                res.code = updated.code;
                return res;
            }
            res.simpleMachineInfo = CasinoBuilder.buildCasinoSimpleMachineInfo(updated.data, timeMillis);
            res.index = req.index;
            res.staffId = req.staffId;
            res.machineId = req.machineId;
            res.endTime = updated.data.getEmploymentMap().get(req.index).getEmploymentEndTime();
            return res;
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求雇员职员异常", e);
        }
        return res;
    }

    /**
     * 请求楼层操作
     *
     * @param player 玩家id
     * @param req    请求
     * @return 响应
     */
    public ResCasinoFloorOperation reqCasinoFloorOperation(Player player, ReqCasinoFloorOperation req) {
        ResCasinoFloorOperation res = new ResCasinoFloorOperation();
        try {
            switch (req.type) {
                case 1 -> {
                    return unlockFloor(player, req, res);
                }
                case 2 -> {
                    return cleanFloor(player.getId(), req, res);
                }
                case 3 -> {
                    return overClean(player.getId(), req, res);
                }
                default -> {
                    res.code = Code.PARAM_ERROR;
                    return res;
                }
            }
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求楼层操作异常", e);
        }
        return res;
    }

    private ResCasinoFloorOperation overClean(long player, ReqCasinoFloorOperation req, ResCasinoFloorOperation res) {
        BuildingFloorCfg buildingFloorCfg = GameDataManager.getBuildingFloorCfg(req.floorId);
        if (Objects.isNull(buildingFloorCfg)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        CommonResult<Long> updated = playerBuildingService.updateData(player, req.casinoId, (result, playerBuilding) -> {
            CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
            if (Objects.isNull(casinoInfo)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            Long buildingCleaningEndTime = casinoInfo.getBuildingCleaningEndTime().get(req.floorId);
            long timeMillis = System.currentTimeMillis();
            if (Objects.isNull(buildingCleaningEndTime) || buildingCleaningEndTime < timeMillis) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //计算消耗
            ItemInfo itemInfo = CasinoBuilder.calculateCostItemInfo(buildingCleaningEndTime, timeMillis);
            if (Objects.isNull(itemInfo)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //扣除消耗
            playerPackService.removeItem(player, itemInfo.itemId, itemInfo.count, "加速清理");
            result.data = timeMillis;
            result.code = Code.SUCCESS;
        });
        if (!updated.success()) {
            res.code = updated.code;
            return res;
        }
        res.cleanEndTime = updated.data;
        return res;
    }

    private ResCasinoFloorOperation cleanFloor(long player, ReqCasinoFloorOperation req, ResCasinoFloorOperation res) {
        BuildingFloorCfg buildingFloorCfg = GameDataManager.getBuildingFloorCfg(req.floorId);
        if (Objects.isNull(buildingFloorCfg)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        CommonResult<Long> updated = playerBuildingService.updateData(player, req.casinoId, (result, playerBuilding) -> {
            long timeMillis = System.currentTimeMillis();
            //获取数据
            CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
            if (Objects.isNull(casinoInfo)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            Long buildingCleaningEndTime = casinoInfo.getBuildingCleaningEndTime().get(req.floorId);
            if (Objects.isNull(buildingCleaningEndTime) || buildingCleaningEndTime < timeMillis) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //更新数据
            buildingCleaningEndTime = System.currentTimeMillis() + buildingFloorCfg.getCleartime() * 1000L;
            result.data = buildingCleaningEndTime;
            result.code = Code.SUCCESS;
        });
        if (!updated.success()) {
            res.code = updated.code;
            return res;
        }
        res.cleanEndTime = updated.data;
        return res;
    }

    private ResCasinoFloorOperation unlockFloor(Player player, ReqCasinoFloorOperation req, ResCasinoFloorOperation res) {
        BuildingFloorCfg buildingFloorCfg = GameDataManager.getBuildingFloorCfg(req.floorId);
        //配置检查
        if (Objects.isNull(buildingFloorCfg) || buildingFloorCfg.getArea() != req.casinoId) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        long playerId = player.getId();
        //检查条件
        BuildingFloorCfg floorCfg = GameDataManager.getBuildingFloorCfg(req.floorId);
        if (Objects.isNull(floorCfg)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        int checked = ConditionUtil.checkCondition(player, floorCfg.getUnlock());
        if (checked != Code.SUCCESS) {
            res.code = checked;
            return res;
        }
        CommonResult<CasinoInfo> updated = playerBuildingService.updateData(playerId, req.casinoId, (result, playerBuilding) -> {

            CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
            if (Objects.isNull(casinoInfo)) {
                casinoInfo = CasinoInfo.getNewCasinoInfo(req.casinoId);
            }
            Map<Integer, List<Long>> buildingData = casinoInfo.getBuildingData();
            int maxUnlockFloorId = getMaxUnlockFloorId(buildingData);
            if (maxUnlockFloorId + 1 != req.floorId) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //创建机台数据
            List<Integer> architectural = floorCfg.getArchitectural();
            List<Long> mId = new ArrayList<>();
            Map<Long, MachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
            for (Integer builderCfgId : architectural) {
                BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(builderCfgId);
                MachineInfo newMachineInfo = MachineInfo.getNewMachineInfo(req.casinoId, cfg);
                machineInfoData.put(newMachineInfo.getId(), newMachineInfo);
                mId.add(newMachineInfo.getId());
            }
            //创建楼层数据
            buildingData.put(req.floorId, mId);
            result.data = casinoInfo;
            result.code = Code.SUCCESS;

        });
        if (!updated.success()) {
            res.code = updated.code;
            return res;
        }
        res.casinoId = req.casinoId;
        res.type = req.type;
        CasinoInfo casinoInfo = updated.data;
        List<Long> mIds = casinoInfo.getBuildingData().get(req.floorId);
        Long cleanEndTime = casinoInfo.getBuildingCleaningEndTime().getOrDefault(req.floorId, 0L);
        res.casinoFloorInfo = CasinoBuilder.buildCasinoFloorInfo(req.floorId, mIds, casinoInfo.getMachineInfoData(), cleanEndTime, System.currentTimeMillis());
        return res;
    }

    public ResCasinoUpgradeMachine reqCasinoUpgradeMachine(Player player, ReqCasinoUpgradeMachine req) {
        ResCasinoUpgradeMachine res = new ResCasinoUpgradeMachine();
        try {
            switch (req.type) {
                //升级
                case 1 -> {
                    return lvUp(player, req, res);
                }
                //快速升级
                case 2 -> {
                    return quickUpgrade(player, req, res);
                }
                default -> {
                    res.code = Code.PARAM_ERROR;
                    return res;
                }
            }
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求楼层操作异常", e);
        }
        return res;
    }

    private ResCasinoUpgradeMachine lvUp(Player player, ReqCasinoUpgradeMachine req, ResCasinoUpgradeMachine res) {
        long playerId = player.getId();
        long timeMillis = System.currentTimeMillis();
        CommonResult<MachineInfo> updated = playerBuildingService.updateData(playerId, req.casinoId, (result, playerBuilding) -> {
            CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
            if (Objects.isNull(casinoInfo)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            Map<Long, MachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
            if (CollectionUtil.isEmpty(machineInfoData)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            MachineInfo machineInfo = machineInfoData.get(req.machineId);
            if (Objects.isNull(machineInfo)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //获取配置
            BuildingFunctionCfg functionCfg = GameDataManager.getBuildingFunctionCfg(machineInfo.getConfigId());
            if (Objects.isNull(functionCfg) || functionCfg.getNextlevelID() <= 0) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //获取下一级id
            BuildingFunctionCfg nextCfg = GameDataManager.getBuildingFunctionCfg(functionCfg.getNextlevelID());
            if (Objects.isNull(nextCfg)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //判断时间

            //判断当前状态
            if (machineInfo.getBuildLvUpEndTime() > timeMillis) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //判断升级条件
            Map<Integer, Integer> condition = functionCfg.getCondition();
            int checked = ConditionUtil.checkCondition(player, condition);
            if (checked != Code.SUCCESS) {
                result.code = checked;
                return;
            }
            int maxLevelType = functionCfg.getMaxLevelType();
            if (maxLevelType > 0) {
                //获取最大等级
                Optional<BuildingFunctionCfg> max = machineInfoData.values().stream()
                        .map(info -> GameDataManager.getBuildingFunctionCfg(info.getConfigId()))
                        .filter(info -> Objects.nonNull(info) && info.getTypeID() == maxLevelType)
                        .min(Comparator.comparingInt(BuildingFunctionCfg::getBuldlevel));
                if (max.isPresent() && functionCfg.getBuldlevel() >= max.get().getBuldlevel()) {
                    result.code = Code.BUILDING_LEVEL_IS_MAX;
                    return;
                }
            }
            //扣除消耗
            CommonResult<PlayerPack> removed = playerPackService.removeItems(playerId, functionCfg.getUplevel_itemid(), "升级建筑");
            if (!removed.success()) {
                result.code = removed.code;
                return;
            }
            //修改数据
            machineInfo.setConfigId(nextCfg.getId());
            machineInfo.setBuildLvUpStartTime(timeMillis);
            machineInfo.setBuildLvUpEndTime(timeMillis + nextCfg.getUptime());
            result.data = machineInfo;
            result.code = Code.SUCCESS;
        });
        if (!updated.success()) {
            res.code = updated.code;
            return res;
        }
        //构建响应
        res.type = req.type;
        res.buildLvUpEndTime = updated.data.getBuildLvUpEndTime();
        res.configId = updated.data.getConfigId();
        res.itemInfo = CasinoBuilder.calculateCostItemInfo(res.buildLvUpEndTime, timeMillis);
        res.machineId = updated.data.getId();
        return res;
    }


    private ResCasinoUpgradeMachine quickUpgrade(Player player, ReqCasinoUpgradeMachine req, ResCasinoUpgradeMachine res) {
        long playerId = player.getId();
        long timeMillis = System.currentTimeMillis();
        CommonResult<Map<Long, MachineInfo>> updated = playerBuildingService.updateData(playerId, req.casinoId, (result, playerBuilding) -> {
            CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
            if (Objects.isNull(casinoInfo)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            Map<Long, MachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
            if (Objects.isNull(machineInfoData)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            MachineInfo machineInfo = machineInfoData.get(req.machineId);
            if (Objects.isNull(machineInfo)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            long buildLvUpEndTime = machineInfo.getBuildLvUpEndTime();
            if (buildLvUpEndTime < timeMillis) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //计算消耗
            ItemInfo itemInfo = CasinoBuilder.calculateCostItemInfo(buildLvUpEndTime, timeMillis);
            if (Objects.isNull(itemInfo)) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //扣除消耗
            playerPackService.removeItem(playerId, itemInfo.itemId, itemInfo.count, "加速升级");
            machineInfo.setBuildLvUpEndTime(timeMillis);
            result.data = machineInfoData;
            result.code = Code.SUCCESS;
        });
        if (!updated.success()) {
            res.code = updated.code;
            return res;
        }
        //构建响应
        Map<Long, MachineInfo> data = updated.data;
        res.type = req.type;
        res.buildLvUpEndTime = timeMillis;
        res.configId = data.get(req.machineId).getConfigId();
        res.machineId = req.machineId;
        return res;
    }

}
