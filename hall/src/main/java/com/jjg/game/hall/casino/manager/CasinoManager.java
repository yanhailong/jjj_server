package com.jjg.game.hall.casino.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ConditionUtil;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.casino.data.*;
import com.jjg.game.hall.casino.logger.CasinoLogger;
import com.jjg.game.hall.casino.pb.CasinoBuilder;
import com.jjg.game.hall.casino.pb.bean.CasinoFloorInfo;
import com.jjg.game.hall.casino.pb.bean.CasinoRewardsInfo;
import com.jjg.game.hall.casino.pb.bean.CasinoSimpleInfo;
import com.jjg.game.hall.casino.pb.req.*;
import com.jjg.game.hall.casino.pb.res.*;
import com.jjg.game.hall.casino.service.PlayerBuildingService;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.utils.GlobalDataCache;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingFloorCfg;
import com.jjg.game.sampledata.bean.BuildingFunctionCfg;
import com.jjg.game.sampledata.bean.DealerFunctionCfg;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.jjg.game.common.utils.TimeHelper.ONE_MINUTE_OF_MILLIS;

/**
 * @author lm
 * @date 2025/8/18 16:24
 */
@Component
public class CasinoManager implements TimerListener<String>, SessionCloseListener, IPlayerLoginSuccess {
    private final Logger log = LoggerFactory.getLogger(CasinoManager.class);
    private final PlayerBuildingService playerBuildingService;
    private final PlayerPackService playerPackService;
    private final CasinoLogger casinoLogger;
    private final CorePlayerService corePlayerService;
    private final TimerCenter timerCenter;
    private final NodeManager nodeManager;
    private final Map<Long, Map<Integer, PlayerBuilding>> dataMap = new ConcurrentHashMap<>();
    private final Map<Long, PlayerController> playerControllerMap = new ConcurrentHashMap<>();
    private final ClusterSystem clusterSystem;
    private TimerEvent<String> casinoSave;
    private TimerEvent<String> casinoCheck;

    public CasinoManager(PlayerBuildingService playerBuildingService,
                         PlayerPackService playerPackService, CasinoLogger casinoLogger, CorePlayerService corePlayerService,
                         TimerCenter timerCenter,
                         NodeManager nodeManager,
                         ClusterSystem clusterSystem) {
        this.playerBuildingService = playerBuildingService;
        this.playerPackService = playerPackService;
        this.casinoLogger = casinoLogger;
        this.corePlayerService = corePlayerService;
        this.timerCenter = timerCenter;
        this.nodeManager = nodeManager;
        this.clusterSystem = clusterSystem;
    }

    @PostConstruct
    public void init() {
        casinoSave = new TimerEvent<>(this, "CasinoSave", 5).withTimeUnit(TimeUnit.MINUTES);
        timerCenter.add(casinoSave);
        casinoCheck = new TimerEvent<>(this, "CasinoCheck", 1).withTimeUnit(TimeUnit.SECONDS);
        timerCenter.add(casinoCheck);
    }

    /**
     * 获取玩家的赌场信息
     *
     * @param playerId 玩家id
     * @param casinoId 赌场id
     * @return 赌场信息
     */
    public CasinoInfo getCasinoInfo(long playerId, int casinoId) {
        Map<Integer, PlayerBuilding> playerBuildingMap = dataMap.get(playerId);
        if (Objects.nonNull(playerBuildingMap) && playerBuildingMap.containsKey(casinoId)) {
            return playerBuildingMap.get(casinoId).getCasinoInfo();
        }
        return null;
    }

    public void shutdown() {
        saveCasinoInfo();
    }

    /**
     * 请求购买一键领取
     *
     * @param playerController 玩家
     * @param req              请求
     * @return 响应
     */
    public ResCasinoBuyClaimAllRewards reqCasinoBuyClaimAllRewards(PlayerController playerController,
                                                                   ReqCasinoBuyClaimAllRewards req) {
        ResCasinoBuyClaimAllRewards res = new ResCasinoBuyClaimAllRewards();
        res.casinoId = req.casinoId;
        Player player = playerController.getPlayer();
        long playerId = player.getId();
        try {
            CasinoInfo casinoInfo = getCasinoInfo(playerId, req.casinoId);
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            //检查消耗
            Pair<Item, Integer> buyClaimAllRewardsConsumer = GlobalDataCache.getBuyClaimAllRewardsConsumer();
            long oneClickClaimEndTime = casinoInfo.getOneClickClaimEndTime();
            if (oneClickClaimEndTime > System.currentTimeMillis()) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            //扣除道具
            CommonResult<ItemOperationResult> result = playerPackService.removeItem(playerController.playerId(),
                    buyClaimAllRewardsConsumer.getFirst(), AddType.ONE_CLICK_UPGRADE_PURCHASE);
            if (!result.success()) {
                res.code = result.code;
                return res;
            }
            //添加数据
            long endTime = System.currentTimeMillis() + buyClaimAllRewardsConsumer.getSecond() * ONE_MINUTE_OF_MILLIS;
            casinoInfo.setOneClickClaimEndTime(endTime);
            casinoInfo.setChange(true);
            res.endTime = casinoInfo.getOneClickClaimEndTime();
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
     * @param req 请求
     * @return 赌场信息
     */
    public ResCasinoInfo reqCasinoInfo(PlayerController playerController, ReqCasinoInfo req) {
        Player player = playerController.getPlayer();
        long playerId = player.getId();
        ResCasinoInfo res = new ResCasinoInfo();
        Map<Integer, PlayerBuilding> playerBuildingMap = dataMap.computeIfAbsent(playerId,
                key -> new ConcurrentHashMap<>());
        PlayerBuilding playerBuilding = playerBuildingMap.get(req.casinoId);
        if (Objects.isNull(playerBuilding)) {
            playerBuilding = playerBuildingService.getCasinoInfoFromAllDB(playerId, req.casinoId);
            if (Objects.isNull(playerBuilding)) {
                playerBuilding = new PlayerBuilding();
                playerBuilding.setCasinoId(req.casinoId);
                playerBuilding.setPlayerId(playerId);
                playerBuilding.setCasinoInfo(CasinoInfo.getNewCasinoInfo(req.casinoId));
            }
            playerBuildingMap.put(req.casinoId, playerBuilding);
            playerControllerMap.put(playerId, playerController);
            playerBuildingService.setLastNode(playerId, nodeManager.getNodePath());
        }
        CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
        long timeMillis = System.currentTimeMillis();
        if (Objects.nonNull(casinoInfo)) {
            CasinoInfo newCasinoInfo = playerBuilding.getCasinoInfo();
            //检查是否有可解锁的楼层
            autoUnlockFloor(req.casinoId, newCasinoInfo, player, timeMillis);
            res.claimAllRewardsEndTime = casinoInfo.getOneClickClaimEndTime();
            Pair<Item, Integer> buyClaimAllRewardsConsumer = GlobalDataCache.getBuyClaimAllRewardsConsumer();
            Item item = buyClaimAllRewardsConsumer.getFirst();
            res.itemInfo = CasinoBuilder.buildItemInfo(item);
            res.casinoId = req.casinoId;
            Map<Integer, List<Long>> map = casinoInfo.getBuildingData();
            res.maxUnlockFloorId = getMaxUnlockFloorId(map);
            //构建全部楼层信息
            res.casinoFloorInfos = new ArrayList<>();
            for (Map.Entry<Integer, List<Long>> entry : map.entrySet()) {
                //构建楼层信息
                CasinoFloorInfo casinoFloorInfo = CasinoBuilder.buildCasinoFloorInfo(newCasinoInfo, entry.getKey(),
                        timeMillis, true);
                res.casinoFloorInfos.add(casinoFloorInfo);
            }
        }
        return res;
    }

    /**
     * 解锁楼层
     *
     * @param casinoId      赌场id
     * @param newCasinoInfo 赌场信息
     * @param player        玩家
     * @param timeMillis    时间戳
     */
    private void autoUnlockFloor(int casinoId, CasinoInfo newCasinoInfo, Player player, long timeMillis) {
        Map<Integer, List<Long>> buildingData = newCasinoInfo.getBuildingData();
        List<BuildingFloorCfg> cfgList = GameDataManager.getBuildingFloorCfgList().stream()
                .filter(cfg -> cfg.getArea() == casinoId)
                .toList();
        int maxUnlockFloorId = getMaxUnlockFloorId(buildingData);
        for (BuildingFloorCfg floorCfg : cfgList) {
            if (floorCfg.getId() < maxUnlockFloorId + 1) {
                continue;
            }
            if (ConditionUtil.checkCondition(player, floorCfg.getUnlock()) != Code.SUCCESS) {
                continue;
            }
            if (floorCfg.getId() == 1000) {
                newCasinoInfo.getBuildingCleaningEndTime().put(floorCfg.getId(), timeMillis);
            }
            //创建机台数据
            List<Integer> architectural = floorCfg.getArchitectural();
            List<Long> mId = new ArrayList<>();
            Map<Long, CasinoMachineInfo> machineInfoData = newCasinoInfo.getMachineInfoData();
            for (Integer builderCfgId : architectural) {
                BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(builderCfgId);
                CasinoMachineInfo newCasinoMachineInfo = CasinoMachineInfo.getNewMachineInfo(cfg);
                machineInfoData.put(newCasinoMachineInfo.getId(), newCasinoMachineInfo);
                mId.add(newCasinoMachineInfo.getId());
            }
            //创建楼层数据
            buildingData.put(floorCfg.getId(), mId);
        }
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
                .orElse(999);
    }

    /**
     * 请求一键领取
     *
     * @param playerController 玩家控制器
     * @param req              请求
     * @return 一键领取结果
     */
    public ResCasinoClaimRewards reqCasinoClaimAllRewards(PlayerController playerController,
                                                          ReqCasinoClaimAllRewards req) {
        ResCasinoClaimRewards res = new ResCasinoClaimRewards();
        long playerId = playerController.playerId();
        try {
            CasinoInfo casinoInfo = getCasinoInfo(playerId, req.casinoId);
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            res.machineId = 0;
            List<CasinoSimpleInfo> casinoSimpleInfos = new ArrayList<>();
            res.casinoSimpleInfos = casinoSimpleInfos;
            Map<Long, CasinoMachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
            long timeMillis = System.currentTimeMillis();
            Map<Integer, Long> getReward = new HashMap<>();
            res.casinoRewardsInfos = new ArrayList<>();
            if (CollectionUtil.isNotEmpty(machineInfoData)) {
                List<TimeNodeData> areaAdd = CasinoBuilder.getTimeNodeData(machineInfoData, timeMillis);
                //计算总收获
                for (CasinoMachineInfo casinoMachineInfo : machineInfoData.values()) {
                    if (casinoMachineInfo.getProfitStartTime() == 0) {
                        continue;
                    }
                    BuildingFunctionCfg cfg =
                            GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
                    if (CollectionUtil.isEmpty(cfg.getOutput())) {
                        continue;
                    }
                    long totalNum = CasinoBuilder.getTotalNum(areaAdd, casinoMachineInfo, cfg, timeMillis);
                    if (totalNum == 0) {
                        continue;
                    }
                    CasinoRewardsInfo casinoRewardsInfo = new CasinoRewardsInfo();
                    casinoRewardsInfo.machineId = casinoMachineInfo.getId();
                    casinoRewardsInfo.itemInfo = ItemUtils.buildItemInfo(cfg.getOutput().get(1), totalNum);
                    res.casinoRewardsInfos.add(casinoRewardsInfo);
                    getReward.merge(cfg.getOutput().get(1), totalNum, Long::sum);
                    //修改数据
                    casinoMachineInfo.setProfitStartTime(timeMillis);
                    casinoMachineInfo.setLastProfit(0);
                    casinoSimpleInfos.add(CasinoBuilder.buildCasinoSimpleMachineInfo(casinoInfo, casinoMachineInfo, timeMillis));
                    casinoInfo.setChange(true);
                }
                //发奖
                CommonResult<ItemOperationResult> result = playerPackService.addItems(playerController.playerId(), getReward,
                        AddType.ONE_CLICK_CLAIM_GAMB_EARNINGS);
                if (!result.success()) {
                    res.code = result.code;
                    return res;
                }
                casinoLogger.sendCasinoRewardsLog(playerId, getReward, result.data);
            }
            res.itemInfos = ItemUtils.buildItemInfo(getReward);
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求一键领取机台收益异常", e);
        }
        return res;
    }


    /**
     * 领取机台收益
     *
     * @param playerController 玩家控制器
     * @param req              请求
     * @return 领取机台收益结果
     */
    public ResCasinoClaimRewards reqCasinoClaimRewards(PlayerController playerController, ReqCasinoClaimRewards req) {
        ResCasinoClaimRewards res = new ResCasinoClaimRewards();
        long playerId = playerController.playerId();
        try {
            CasinoInfo casinoInfo = getCasinoInfo(playerId, req.casinoId);
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            res.machineId = 0;
            List<CasinoSimpleInfo> casinoSimpleInfos = new ArrayList<>();
            res.casinoSimpleInfos = casinoSimpleInfos;
            Map<Long, CasinoMachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
            CasinoMachineInfo casinoMachineInfo = machineInfoData.get(req.machineId);
            if (Objects.isNull(casinoMachineInfo) || casinoMachineInfo.getProfitStartTime() == 0) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            long timeMillis = System.currentTimeMillis();
            BuildingFunctionCfg cfg =
                    GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
            if (CollectionUtil.isEmpty(cfg.getOutput())) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            List<TimeNodeData> areaAdd = CasinoBuilder.getTimeNodeData(machineInfoData, timeMillis);
            //计算总收获
            long totalNum = CasinoBuilder.getTotalNum(areaAdd, casinoMachineInfo, cfg, timeMillis);
            casinoMachineInfo.setProfitStartTime(timeMillis);
            casinoMachineInfo.setLastProfit(0);
            casinoInfo.setChange(true);
            //发奖
            Item item = new Item(cfg.getOutput().get(1), totalNum);
            CommonResult<ItemOperationResult> result = playerPackService.addItem(playerController.playerId(), item.getId(), item.getItemCount(),
                    AddType.ONE_CLICK_CLAIM_TABKE_EARNINGS);
            if (!result.success()) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            casinoLogger.sendCasinoRewardsLog(playerId, Map.of(item.getId(), item.getItemCount()), result.data);
            casinoSimpleInfos.add(CasinoBuilder.buildCasinoSimpleMachineInfo(casinoInfo, casinoMachineInfo,
                    timeMillis));
            res.machineId = req.machineId;
            res.itemInfos = new ArrayList<>();
            res.itemInfos.add(CasinoBuilder.buildItemInfo(item));
            res.casinoRewardsInfos = new ArrayList<>();
            CasinoRewardsInfo casinoRewardsInfo = new CasinoRewardsInfo();
            casinoRewardsInfo.machineId = req.machineId;
            casinoRewardsInfo.itemInfo = CasinoBuilder.buildItemInfo(item);
            res.casinoRewardsInfos.add(casinoRewardsInfo);
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
     * @param playerController 玩家控制器
     * @param req              请求
     * @return 响应
     */
    public ResCasinoEmployStaff reqCasinoEmployStaff(PlayerController playerController, ReqCasinoEmployStaff req) {
        ResCasinoEmployStaff res = new ResCasinoEmployStaff();
        long playerId = playerController.playerId();
        try {
            CasinoInfo casinoInfo = getCasinoInfo(playerId, req.casinoId);
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            CasinoMachineInfo casinoMachineInfo = casinoInfo.getMachineInfoData().get(req.machineId);
            if (Objects.isNull(casinoMachineInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            long timeMillis = System.currentTimeMillis();
            BuildingFunctionCfg cfg =
                    GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
            if (Objects.isNull(cfg)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            DealerFunctionCfg dealerFunctionCfg = GameDataManager.getDealerFunctionCfg(req.staffId);
            if (Objects.isNull(dealerFunctionCfg)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            Map<Integer, CasinoEmployment> employmentMap = casinoMachineInfo.getEmploymentMap();
            if (Objects.isNull(employmentMap)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            CasinoEmployment casinoEmployment = employmentMap.getOrDefault(req.index, new CasinoEmployment());
            List<Integer> cost = dealerFunctionCfg.getHiringExpenses();
            Item costItem = new Item(cost.getFirst(), cost.getLast());
            CommonResult<ItemOperationResult> result = playerPackService.removeItem(playerController.playerId(), costItem, AddType.EMPLOYEE_STAFF);
            if (!result.success()) {
                res.code = result.code;
                return res;
            }
            //如果之前已经停止受益了 计算停止的收益
            if (casinoEmployment.getEmploymentEndTime() > timeMillis) {
                //计算之前的收益
                List<TimeNodeData> areaAdd = CasinoBuilder.getTimeNodeData(casinoInfo.getMachineInfoData(), timeMillis);
                long totalNum = CasinoBuilder.getTotalNum(areaAdd, casinoMachineInfo, cfg, timeMillis);
                casinoMachineInfo.addLastProfit(totalNum);
            }
            employmentMap.put(req.index, casinoEmployment);
            //设置数据
            casinoEmployment.setEmploymentStartTime(timeMillis);
            casinoEmployment.setEmploymentId(req.staffId);
            casinoEmployment.setEmploymentEndTime(timeMillis + dealerFunctionCfg.getDuration() * 1000L);
            casinoEmployment.setId(dealerFunctionCfg.getId());
            casinoInfo.setChange(true);
            int floorId = getFloorId(casinoInfo, casinoMachineInfo);
            BuildingFunctionCfg functionCfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
            casinoLogger.sendCasinoOperationLog(playerController.playerId(), floorId, 5, functionCfg.getTypeID(), 0,
                    Map.of(costItem.getId(), costItem.getItemCount()), result.data, 0);
            res.simpleMachineInfo = CasinoBuilder.buildCasinoSimpleMachineInfo(casinoInfo, casinoMachineInfo, timeMillis);
            res.index = req.index;
            res.staffId = req.staffId;
            res.machineId = req.machineId;
            res.endTime = casinoEmployment.getEmploymentEndTime();
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
     * @param playerController 玩家控制器
     * @param req              请求
     * @return 响应
     */
    public ResCasinoFloorOperation reqCasinoFloorOperation(PlayerController playerController,
                                                           ReqCasinoFloorOperation req) {
        ResCasinoFloorOperation res = new ResCasinoFloorOperation();
        Player player = playerController.getPlayer();
        try {
            CasinoInfo casinoInfo = getCasinoInfo(player.getId(), req.casinoId);
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            if (!casinoInfo.getBuildingData().containsKey(req.floorId)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            switch (req.type) {
                case 2 -> {
                    return cleanFloor(player, req, res, casinoInfo);
                }
                case 3 -> {
                    return overClean(playerController, req, res, casinoInfo);
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

    private ResCasinoFloorOperation overClean(PlayerController playerController, ReqCasinoFloorOperation req,
                                              ResCasinoFloorOperation res, CasinoInfo casinoInfo) {
        Player player = playerController.getPlayer();
        BuildingFloorCfg buildingFloorCfg = GameDataManager.getBuildingFloorCfg(req.floorId);
        if (Objects.isNull(buildingFloorCfg)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        long timeMillis = System.currentTimeMillis();
        Long buildingCleaningEndTime = casinoInfo.getBuildingCleaningEndTime().get(req.floorId);
        if (Objects.isNull(buildingCleaningEndTime) || buildingCleaningEndTime < timeMillis) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //计算消耗
        Item item = CasinoBuilder.calculateCostItemInfo(buildingCleaningEndTime, timeMillis);
        if (Objects.isNull(item)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //扣除消耗
        CommonResult<ItemOperationResult> result = playerPackService.removeItem(playerController.playerId(), item, AddType.CLEANUP_PROCESS);
        if (!result.success()) {
            res.code = result.code;
            return res;
        }
        casinoInfo.getBuildingCleaningEndTime().put(req.floorId, timeMillis);
        casinoInfo.setChange(true);
        casinoLogger.sendCasinoOperationLog(player.getId(), req.floorId, 2, 0, buildingCleaningEndTime - timeMillis,
                Map.of(item.getId(), item.getItemCount()), result.data, 0);
        res.casinoFloorInfo = CasinoBuilder.buildCasinoFloorInfo(casinoInfo, req.floorId, timeMillis, true);
        res.casinoId = req.casinoId;
        res.type = req.type;
        return res;
    }

    /**
     * 清理楼层
     */
    private ResCasinoFloorOperation cleanFloor(Player player, ReqCasinoFloorOperation req, ResCasinoFloorOperation res, CasinoInfo casinoInfo) {
        BuildingFloorCfg buildingFloorCfg = GameDataManager.getBuildingFloorCfg(req.floorId);
        if (Objects.isNull(buildingFloorCfg)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        Long buildingCleaningEndTime = casinoInfo.getBuildingCleaningEndTime().getOrDefault(req.floorId, 0L);
        if (buildingCleaningEndTime != 0) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //更新数据
        buildingCleaningEndTime = System.currentTimeMillis() + buildingFloorCfg.getCleartime() * 1000L;
        casinoInfo.getBuildingCleaningEndTime().put(req.floorId, buildingCleaningEndTime);
        casinoInfo.setChange(true);
        casinoLogger.sendCasinoOperationLog(player.getId(), req.floorId, 1, 0, 0,
                null, null, 0);
        res.casinoFloorInfo = CasinoBuilder.buildCasinoFloorInfo(casinoInfo, req.floorId, System.currentTimeMillis(), false);
        res.casinoId = req.casinoId;
        res.type = req.type;
        return res;
    }

    /**
     * @param playerController 玩家控制器
     * @param req              请求
     */
    public ResCasinoUpgradeMachine reqCasinoUpgradeMachine(PlayerController playerController,
                                                           ReqCasinoUpgradeMachine req) {
        ResCasinoUpgradeMachine res = new ResCasinoUpgradeMachine();
        Player player = playerController.getPlayer();
        player = corePlayerService.get(player.getId());
        playerController.setPlayer(player);
        try {
            CasinoInfo casinoInfo = getCasinoInfo(player.getId(), req.casinoId);
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            switch (req.type) {
                //升级
                case 1 -> {
                    return lvUp(player, req, res, casinoInfo);
                }
                //快速升级
                case 2 -> {
                    return quickUpgrade(playerController, req, res, casinoInfo);
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


    private ResCasinoUpgradeMachine lvUp(Player player, ReqCasinoUpgradeMachine req, ResCasinoUpgradeMachine res,
                                         CasinoInfo casinoInfo) {
        long playerId = player.getId();
        long timeMillis = System.currentTimeMillis();
        Map<Long, CasinoMachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
        if (CollectionUtil.isEmpty(machineInfoData)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        CasinoMachineInfo casinoMachineInfo = machineInfoData.get(req.machineId);
        if (Objects.isNull(casinoMachineInfo)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //获取配置
        BuildingFunctionCfg functionCfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
        if (Objects.isNull(functionCfg) || functionCfg.getNextlevelID() <= 0) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //获取下一级id
        BuildingFunctionCfg nextCfg = GameDataManager.getBuildingFunctionCfg(functionCfg.getNextlevelID());
        if (Objects.isNull(nextCfg)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }

        //判断当前状态
        if (casinoMachineInfo.getBuildLvUpEndTime() > timeMillis) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //判断升级条件
        Map<Integer, Integer> condition = functionCfg.getCondition();
        int checked = ConditionUtil.checkCondition(player, condition);
        if (checked != Code.SUCCESS) {
            res.code = checked;
            return res;
        }
        int maxLevelType = functionCfg.getMaxLevelType();
        if (maxLevelType > 0) {
            //获取最大等级
            Optional<BuildingFunctionCfg> max = machineInfoData.values().stream()
                    .map(info -> GameDataManager.getBuildingFunctionCfg(info.getRealConfigId(timeMillis)))
                    .filter(info -> Objects.nonNull(info) && info.getTypeID() == maxLevelType)
                    .min(Comparator.comparingInt(BuildingFunctionCfg::getBuldlevel));
            if (max.isPresent() && functionCfg.getBuldlevel() >= max.get().getBuldlevel()) {
                res.code = Code.BUILDING_LEVEL_IS_MAX;
                return res;
            }
        }
        //扣除消耗
        CommonResult<ItemOperationResult> removed = playerPackService.removeItems(player, functionCfg.getUplevel_itemid(), AddType.UP_BUILD);
        if (!removed.success()) {
            res.code = removed.code;
            return res;
        }
        //修改数据
        casinoMachineInfo.setLastConfigId(functionCfg.getId());
        casinoMachineInfo.setConfigId(nextCfg.getId());
        casinoMachineInfo.setBuildLvUpStartTime(timeMillis);
        casinoMachineInfo.setBuildLvUpEndTime(timeMillis + functionCfg.getUptime() * 1000L);
        casinoInfo.setChange(true);
        //发送日志
        int floorId = getFloorId(casinoInfo, casinoMachineInfo);
        casinoLogger.sendCasinoOperationLog(playerId, floorId, 3, functionCfg.getTypeID(), 0,
                functionCfg.getUplevel_itemid(), removed.data, nextCfg.getBuldlevel());
        //构建响应
        res.type = req.type;
        res.buildLvUpEndTime = casinoMachineInfo.getBuildLvUpEndTime();
        res.configId = casinoMachineInfo.getRealConfigId(timeMillis);
        res.machineId = casinoMachineInfo.getId();
        return res;
    }


    private ResCasinoUpgradeMachine quickUpgrade(PlayerController playerController, ReqCasinoUpgradeMachine req,
                                                 ResCasinoUpgradeMachine res, CasinoInfo casinoInfo) {
        long timeMillis = System.currentTimeMillis();
        Map<Long, CasinoMachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
        if (Objects.isNull(machineInfoData)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        CasinoMachineInfo casinoMachineInfo = machineInfoData.get(req.machineId);
        if (Objects.isNull(casinoMachineInfo)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        long buildLvUpEndTime = casinoMachineInfo.getBuildLvUpEndTime();
        if (buildLvUpEndTime < timeMillis) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //计算消耗
        Item item = CasinoBuilder.calculateCostItemInfo(buildLvUpEndTime, timeMillis);
        if (Objects.isNull(item)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //扣除消耗
        CommonResult<ItemOperationResult> result = playerPackService.removeItem(playerController.playerId(), item, AddType.UPGRADE_PROCESS);
        if (!result.success()) {
            res.code = result.code;
            return res;
        }
        casinoMachineInfo.setBuildLvUpEndTime(timeMillis);
        if (casinoMachineInfo.getProfitStartTime() == 0) {
            BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
            if (Objects.nonNull(cfg) && CollectionUtil.isNotEmpty(cfg.getOutput())) {
                casinoMachineInfo.setProfitStartTime(timeMillis);
            }
        }
        casinoInfo.setChange(true);
        int floorId = getFloorId(casinoInfo, casinoMachineInfo);
        BuildingFunctionCfg functionCfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
        casinoLogger.sendCasinoOperationLog(playerController.playerId(), floorId, 4, functionCfg.getTypeID(), buildLvUpEndTime - timeMillis,
                Map.of(item.getId(), item.getItemCount()), result.data, functionCfg.getBuldlevel());
        //构建响应
        res.type = req.type;
        res.buildLvUpEndTime = timeMillis;
        res.configId = casinoMachineInfo.getRealConfigId(timeMillis);
        res.machineId = req.machineId;
        return res;
    }

    /**
     * 获取楼层id
     *
     * @param casinoInfo        楼层信息
     * @param casinoMachineInfo 机台信息
     * @return 楼层id
     */
    private int getFloorId(CasinoInfo casinoInfo, CasinoMachineInfo casinoMachineInfo) {
        int floorId = 0;
        for (Map.Entry<Integer, List<Long>> entry : casinoInfo.getBuildingData().entrySet()) {
            if (entry.getValue().contains(casinoMachineInfo.getId())) {
                floorId = entry.getKey();
                break;
            }
        }
        return floorId;
    }

    @Override
    public void onTimer(TimerEvent<String> event) {
        long timeMillis = System.currentTimeMillis();
        //定时回存
        if (event == casinoSave) {
            saveCasinoInfo();
        }
        if (event == casinoCheck) {
            checkTime(timeMillis);
        }
    }

    private void checkTime(long timeMillis) {
        for (Map<Integer, PlayerBuilding> playerBuildingMap : dataMap.values()) {
            for (PlayerBuilding playerBuilding : playerBuildingMap.values()) {
                CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
                Set<Long> changeMachineIds = new HashSet<>();
                boolean allReflush = false;
                Map<Long, CasinoMachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
                for (Map.Entry<Integer, Long> entry : casinoInfo.getBuildingCleaningEndTime().entrySet()) {
                    try {
                        changeMachineIds.clear();
                        long remainTime = timeMillis - entry.getValue();
                        if (remainTime > 0 && remainTime <= 1000) {
                            changeMachineIds.addAll(casinoInfo.getBuildingData().get(entry.getKey()));
                        }
                        if (changeMachineIds.isEmpty()) {
                            List<Long> ids = casinoInfo.getBuildingData().get(entry.getKey());
                            for (Long id : ids) {
                                boolean chenge = false;
                                CasinoMachineInfo casinoMachineInfo = machineInfoData.get(id);
                                remainTime = timeMillis - casinoMachineInfo.getBuildLvUpEndTime();
                                //升级结束 相差1000毫秒时推送一次
                                if (remainTime > 0 && remainTime <= 1000) {
                                    changeMachineIds.add(casinoMachineInfo.getId());
                                    BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
                                    if (HallConstant.Casino.ALL_REFLUSH_TYPE.contains(cfg.getTypeID())) {
                                        allReflush = true;
                                        break;
                                    }
                                }
                                //雇佣结束
                                Map<Integer, CasinoEmployment> employmentMap = casinoMachineInfo.getEmploymentMap();
                                if (CollectionUtil.isNotEmpty(employmentMap)) {
                                    for (CasinoEmployment employment : employmentMap.values()) {
                                        remainTime = timeMillis - employment.getEmploymentEndTime();
                                        if (remainTime > 0 && remainTime <= 1000) {
                                            chenge = true;
                                        }
                                    }
                                }
                                if (chenge) {
                                    changeMachineIds.add(casinoMachineInfo.getId());
                                }
                            }
                        }
                        //构建基本信息
                        if (changeMachineIds.isEmpty()) {
                            continue;
                        }
                        PlayerController playerController = playerControllerMap.get(playerBuilding.getPlayerId());
                        if (Objects.isNull(playerController)) {
                            continue;
                        }
                        if (allReflush) {
                            for (CasinoMachineInfo machineInfo : machineInfoData.values()) {
                                BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(machineInfo.getRealConfigId(timeMillis));
                                if (cfg == null || CollectionUtil.isEmpty(cfg.getOutput())) {
                                    continue;
                                }
                                changeMachineIds.add(machineInfo.getId());
                            }
                        }
                        List<CasinoSimpleInfo> simpleInfos = new ArrayList<>();
                        for (Long changeMachineId : changeMachineIds) {
                            CasinoMachineInfo casinoMachineInfo = machineInfoData.get(changeMachineId);
                            //如果收益开始时间为0设置为当前时间
                            if (casinoMachineInfo.getProfitStartTime() == 0) {
                                BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
                                if (Objects.nonNull(cfg) && CollectionUtil.isNotEmpty(cfg.getOutput())) {
                                    casinoMachineInfo.setProfitStartTime(timeMillis);
                                    casinoInfo.setChange(true);
                                }
                            }
                            CasinoSimpleInfo casinoSimpleInfo = CasinoBuilder.buildCasinoSimpleMachineInfo(casinoInfo
                                    , casinoMachineInfo, timeMillis);
                            simpleInfos.add(casinoSimpleInfo);
                        }
                        NotifyCasinoSimpleChange notifyCasinoSimpleChange = new NotifyCasinoSimpleChange();
                        notifyCasinoSimpleChange.infos = simpleInfos;
                        playerController.send(notifyCasinoSimpleChange);
                    } catch (Exception e) {
                        log.error("我的赌场定时器异常", e);
                    }
                }
            }
        }
    }

    private void saveCasinoInfo() {
        long playerId = 0;
        try {
            for (Map<Integer, PlayerBuilding> playerBuildingMap : dataMap.values()) {
                for (PlayerBuilding playerBuilding : playerBuildingMap.values()) {
                    if (!playerBuilding.getCasinoInfo().isChange()) {
                        continue;
                    }
                    playerId = playerBuilding.getPlayerId();
                    playerBuildingService.redisSave(playerBuilding);
                    log.info("回存我的赌场信息成功 playerId：{} casinoId:{}", playerId, playerBuilding.getCasinoId());
                }
            }
            playerBuildingService.delLastNode(playerId);
        } catch (Exception e) {
            log.error("回存我的赌场信息失败 playerId:{}", playerId, e);
        }
    }


    @Override
    public void sessionClose(PFSession session) {
        if (session.getPlayerId() > 0) {
            saveOnExit(session.playerId);
        }
    }

    private void saveOnExit(long playerId) {
        Map<Integer, PlayerBuilding> playerBuildingMap = dataMap.get(playerId);
        if (CollectionUtil.isNotEmpty(playerBuildingMap)) {
            try {
                for (PlayerBuilding playerBuilding : playerBuildingMap.values()) {
                    if (playerBuilding.getCasinoInfo().isChange()) {
                        log.info("退出回存我的赌场信息 playerId:{} casinoId:{}", playerId, playerBuilding.getCasinoId());
                        playerBuildingService.redisSave(playerBuilding);
                    }
                }
            } catch (Exception e) {
                log.error("退出时保存我的赌场数据失败 playerId:{}", playerId);
            }
        }
        dataMap.remove(playerId);
        playerControllerMap.remove(playerId);
        playerBuildingService.delLastNode(playerId);
    }

    public ResCasinoExit reqCasinoExit(Player player, ReqCasinoExit req) {
        saveOnExit(player.getId());
        return new ResCasinoExit(Code.SUCCESS);
    }

    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, Account account, boolean firstLogin) {
        //我的赌场未保存完成进入新节点 切换到上个节点
        String lastNode = playerBuildingService.getLastNode(playerController.playerId());
        if (StringUtils.isNotEmpty(lastNode)) {
            MarsNode node = clusterSystem.getNode(lastNode);
            if (Objects.nonNull(node)) {
                clusterSystem.switchNode(playerController.getSession(), node);
                log.info("我的赌场 切换到上次未保存完的节点");
            } else {
                log.error("我的赌场信息存在进入未保存完信息的节点");
            }
        }
    }

    @Override
    public int executeOrder() {
        return 99;
    }
}
