package com.jjg.game.hall.casino.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
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
            CommonResult<Long> updated = playerBuildingService.updateData(playerId, (result) -> {
                String path = String.format("$.buildingData.%d.oneClickClaimEndTime", req.casinoId);
                Long oneClickClaimEndTime = playerBuildingService.getDataOfPath(playerId, path, new TypeReference<>() {
                });
                if (Objects.nonNull(oneClickClaimEndTime) && oneClickClaimEndTime > System.currentTimeMillis()) {
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
                playerBuildingService.setDataOfPath(playerId, path, endTime);
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
        PlayerBuilding playerBuilding = playerBuildingService.getFromAllDB(playerId);
        if (Objects.isNull(playerBuilding)) {
            res.code = Code.UNKNOWN_ERROR;
            return res;
        }
        CasinoInfo casinoInfo = playerBuilding.getBuildingData().get(req.casinoId);
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
        String path = String.format("$.buildingData.%d.machineInfoData", req.casinoId);
        ResCasinoClaimRewards res = new ResCasinoClaimRewards();
        try {
            res.machineId = 0;
            List<CasinoSimpleMachineInfo> casinoSimpleMachineInfos = new ArrayList<>();
            res.casinoSimpleMachineInfos = casinoSimpleMachineInfos;
            CommonResult<Map<Integer, Long>> updated = playerBuildingService.updateData(playerId, (result) -> {
                //获取所有机台信息
                Map<Long, MachineInfo> machineInfoData = playerBuildingService.getDataOfPath(playerId, path, new TypeReference<>() {
                });
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
                    //回存
                    playerBuildingService.setDataOfPath(playerId, path, machineInfoData);
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
        String path = String.format("$.buildingData.%d.machineInfoData.%d", req.casinoId, req.machineId);
        ResCasinoClaimRewards res = new ResCasinoClaimRewards();
        try {
            res.machineId = 0;
            List<CasinoSimpleMachineInfo> casinoSimpleMachineInfos = new ArrayList<>();
            res.casinoSimpleMachineInfos = casinoSimpleMachineInfos;
            CommonResult<Item> updated = playerBuildingService.updateData(playerId, (result) -> {
                //获取所有机台信息
                MachineInfo machineInfo = playerBuildingService.getDataOfPath(playerId, path, new TypeReference<>() {
                });
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
                playerBuildingService.setDataOfPath(playerId, path, machineInfo);
                //发奖
                Item item = new Item(cfg.getOutput().getFirst(), totalNum);
                playerPackService.addItem(playerId, item.getId(), item.getCount(), "一键领取机台收益");
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
        String path = String.format("$.buildingData.%d.machineInfoData.%d", req.casinoId, req.machineId);
        long timeMillis = System.currentTimeMillis();
        try {
            CommonResult<MachineInfo> updated = playerBuildingService.updateData(player, (result) -> {
                MachineInfo machineInfo = playerBuildingService.getDataOfPath(player, path, new TypeReference<>() {
                });
                if (Objects.isNull(machineInfo)) {
                    result.code = Code.PARAM_ERROR;
                    return;
                }
                //检查道具是否够
                if (!playerPackService.checkHasItems(player, costItem)) {
                    result.code = Code.NOT_ENOUGH_ITEM;
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
                //回存数据
                playerBuildingService.setDataOfPath(player, path, machineInfo);
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
    public ResCasinoFloorOperation reqCasinoFloorOperation(long player, ReqCasinoFloorOperation req) {
        ResCasinoFloorOperation res = new ResCasinoFloorOperation();
        try {
            switch (req.type) {
                case 1 -> {
                    return unlockFloor(player, req);
                }
                case 2 -> {
                    return cleanFloor(player, req);
                }
                case 3 -> {
                    return overClean(player, req);
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

    private ResCasinoFloorOperation overClean(long player, ReqCasinoFloorOperation req) {
        ResCasinoFloorOperation res = new ResCasinoFloorOperation();
        BuildingFloorCfg buildingFloorCfg = GameDataManager.getBuildingFloorCfg(req.floorId);
        if (Objects.isNull(buildingFloorCfg)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        String path = """
                $.buildingData.%d.buildingCleaningEndTime.%d""".formatted(req.casinoId, req.floorId);
        playerBuildingService.updateData(player,(result)->{

        });
        return res;
    }

    private ResCasinoFloorOperation cleanFloor(long player, ReqCasinoFloorOperation req) {
        ResCasinoFloorOperation res = new ResCasinoFloorOperation();
        BuildingFloorCfg buildingFloorCfg = GameDataManager.getBuildingFloorCfg(req.floorId);
        if (Objects.isNull(buildingFloorCfg)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        String path = """
                $.buildingData.%d.buildingCleaningEndTime.%d""".formatted(req.casinoId, req.floorId);
        CommonResult<Long> updated = playerBuildingService.updateData(player, (result) -> {
            long timeMillis = System.currentTimeMillis();
            //获取数据
            Long buildingCleaningEndTime = playerBuildingService.getDataOfPath(player, path, new TypeReference<>() {
            });
            if (Objects.isNull(buildingCleaningEndTime) || buildingCleaningEndTime < timeMillis) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            //更新数据
            buildingCleaningEndTime = System.currentTimeMillis() + buildingFloorCfg.getCleartime() * 1000L;
            playerBuildingService.setDataOfPath(player, path, buildingCleaningEndTime);
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

    private ResCasinoFloorOperation unlockFloor(long player, ReqCasinoFloorOperation req) {
        ResCasinoFloorOperation res = new ResCasinoFloorOperation();
        BuildingFloorCfg buildingFloorCfg = GameDataManager.getBuildingFloorCfg(req.floorId);
        //配置检查
        if (Objects.isNull(buildingFloorCfg) || buildingFloorCfg.getArea() != req.casinoId) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        String path = String.format("$.buildingData.%d.buildingData", req.casinoId);
        CommonResult<Integer> updated = playerBuildingService.updateData(player, (result) -> {
            Map<Integer, List<Long>> buildingData = playerBuildingService.getDataOfPath(player, path, new TypeReference<>() {
            });
            int maxUnlockFloorId = getMaxUnlockFloorId(buildingData);
            if (maxUnlockFloorId + 1 != req.floorId) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            BuildingFloorCfg floorCfg = GameDataManager.getBuildingFloorCfg(maxUnlockFloorId);
            //检查消耗
            List<Integer> unlock = floorCfg.getUnlock();
            if (unlock.size() != 2) {
                result.code = Code.PARAM_ERROR;
                return;
            }
            Item item = new Item(unlock.getFirst(), unlock.getLast());
            if (!playerPackService.checkHasItems(player, item)) {
                result.code = Code.NOT_ENOUGH_ITEM;
                return;
            }
            //扣除道具
            CommonResult<PlayerPack> removed = playerPackService.removeItem(player, item, "解锁楼层");
            if (!removed.success()) {
                result.code = Code.NOT_ENOUGH_ITEM;
                return;
            }
            //创建楼层数据
            buildingData.put(req.floorId, new ArrayList<>());
            //保存数据
            playerBuildingService.setDataOfPath(player, path, buildingData);
            result.data = floorCfg.getId();
            result.code = Code.SUCCESS;

        });
        if (!updated.success()) {
            res.code = updated.code;
            return res;
        }
        res.casinoId = req.casinoId;
        res.type = req.type;
        res.casinoFloorInfo = CasinoBuilder.buildCasinoFloorInfo(req.floorId, null, null, 0, System.currentTimeMillis());
        return res;
    }

}
