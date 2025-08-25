package com.jjg.game.hall.casino.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.hall.casino.data.*;
import com.jjg.game.hall.casino.pb.CasinoBuilder;
import com.jjg.game.hall.casino.pb.bean.CasinoFloorInfo;
import com.jjg.game.hall.casino.pb.bean.CasinoSimpleInfo;
import com.jjg.game.hall.casino.pb.req.*;
import com.jjg.game.hall.casino.pb.res.*;
import com.jjg.game.hall.casino.service.PlayerBuildingService;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.struct.ItemInfo;
import com.jjg.game.hall.utils.ConditionUtil;
import com.jjg.game.hall.utils.GlobalDataCache;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingFloorCfg;
import com.jjg.game.sampledata.bean.BuildingFunctionCfg;
import com.jjg.game.sampledata.bean.BuildingGainCfg;
import com.jjg.game.sampledata.bean.DealerFunctionCfg;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author lm
 * @date 2025/8/18 16:24
 */
@Component
public class CasinoManager implements TimerListener<String>, SessionCloseListener {
    private final Logger log = LoggerFactory.getLogger(CasinoManager.class);
    @Autowired
    private PlayerBuildingService playerBuildingService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private TimerCenter timerCenter;
    private final Map<Long, PlayerBuilding> dataMap = new ConcurrentHashMap<>();
    private final Map<Long, PlayerBuilding> changeDataMap = new ConcurrentHashMap<>();
    private final Map<Long, PlayerController> playerControllerMap = new ConcurrentHashMap<>();
    private TimerEvent<String> casinoSave;
    private TimerEvent<String> casinoCheck;


    @PostConstruct
    public void init() {
        casinoSave = new TimerEvent<>(this, "CasinoSave", 5).withTimeUnit(TimeUnit.MINUTES);
        timerCenter.add(casinoSave);
        casinoCheck = new TimerEvent<>(this, "CasinoCheck", 1).withTimeUnit(TimeUnit.SECONDS);
        timerCenter.add(casinoCheck);
    }

    public CasinoInfo getCasinoInfo(long playerId) {
        PlayerBuilding playerBuilding = dataMap.get(playerId);
        if (Objects.nonNull(playerBuilding)) {
            return playerBuilding.getCasinoInfo();
        }
        return null;
    }

    /**
     * 请求购买一键领取
     *
     * @param player 玩家
     * @param req    请求
     * @return 响应
     */
    public ResCasinoBuyClaimAllRewards reqCasinoBuyClaimAllRewards(Player player, ReqCasinoBuyClaimAllRewards req) {
        ResCasinoBuyClaimAllRewards res = new ResCasinoBuyClaimAllRewards();
        res.casinoId = req.casinoId;
        long playerId = player.getId();
        try {
            CasinoInfo casinoInfo = getCasinoInfo(playerId);
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            //检查消耗
            PlayerPack pack = playerPackService.getFromAllDB(playerId);
            if (Objects.isNull(pack)) {
                res.code = Code.NOT_ENOUGH_ITEM;
                return res;
            }
            Pair<Item, Integer> buyClaimAllRewardsConsumer = GlobalDataCache.getBuyClaimAllRewardsConsumer();
            if (!playerPackService.checkHasItems(player, List.of(buyClaimAllRewardsConsumer.getFirst()))) {
                res.code = Code.NOT_ENOUGH_ITEM;
                return res;
            }
            long oneClickClaimEndTime = casinoInfo.getOneClickClaimEndTime();
            if (oneClickClaimEndTime > System.currentTimeMillis()) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            //扣除道具
            CommonResult<PlayerPack> removed = playerPackService.removeItem(playerId, buyClaimAllRewardsConsumer.getFirst(), "一键升级购买");
            if (!removed.success()) {
                res.code = Code.NOT_ENOUGH_ITEM;
                return res;
            }
            //添加数据
            long endTime = System.currentTimeMillis() + buyClaimAllRewardsConsumer.getSecond() * 1000;
            casinoInfo.setOneClickClaimEndTime(endTime);
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
        PlayerBuilding playerBuilding = dataMap.get(playerId);
        if (Objects.isNull(playerBuilding)) {
            playerBuilding = playerBuildingService.getCasinoInfoFromAllDB(playerId, req.casinoId);
            if (Objects.isNull(playerBuilding)) {
                playerBuilding = new PlayerBuilding();
                playerBuilding.setCasinoId(req.casinoId);
                playerBuilding.setPlayerId(playerId);
                playerBuilding.setCasinoInfo(CasinoInfo.getNewCasinoInfo(req.casinoId));
            }
            dataMap.put(playerId, playerBuilding);
            playerControllerMap.put(playerId, playerController);
        }
        CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
        long timeMillis = System.currentTimeMillis();
        if (Objects.nonNull(casinoInfo)) {
            //检查是否有可解锁的楼层 TODO 移动到玩家升级
            CasinoInfo newCasinoInfo = playerBuilding.getCasinoInfo();
            Map<Integer, List<Long>> buildingData = newCasinoInfo.getBuildingData();
            List<BuildingFloorCfg> cfgList = GameDataManager.getBuildingFloorCfgList().stream()
                    .filter(cfg -> cfg.getArea() == req.casinoId)
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
                CasinoFloorInfo casinoFloorInfo = CasinoBuilder.buildCasinoFloorInfo(newCasinoInfo, entry.getKey(), timeMillis, true);
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
                .orElse(999);
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
            CasinoInfo casinoInfo = getCasinoInfo(playerId);
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
            if (CollectionUtil.isNotEmpty(machineInfoData)) {
                //总加成
                List<TimeNodeData> areaAdd = new ArrayList<>();
                //获取提款机等级
                for (CasinoMachineInfo casinoMachineInfo : machineInfoData.values()) {
                    BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
                    if (Objects.isNull(cfg)) {
                        continue;
                    }
                    if (cfg.getTypeID() == HallConstant.Casino.REST_AREA_TYPE || cfg.getTypeID() == HallConstant.Casino.WITHDRAWAL_AREA_TYPE) {
                        BuildingGainCfg buildingGainCfg = GameDataManager.getBuildingGainCfg(cfg.getBuffid());
                        if (Objects.isNull(buildingGainCfg) || buildingGainCfg.getBufftype() == 0) {
                            continue;
                        }
                        BuildingFunctionCfg functionCfg = CasinoBuilder.getLastBuildingFunctionCfg(buildingGainCfg.getId());
                        areaAdd.add(TimeNodeData.getNewTimeNodeData(casinoMachineInfo, Objects.isNull(functionCfg) ? 0 : functionCfg.getId()));
                    }
                }
                //获取休息区等级
                //计算总收获
                for (CasinoMachineInfo casinoMachineInfo : machineInfoData.values()) {
                    BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
                    if (cfg.getSavenum() == 0) {
                        continue;
                    }
                    long totalNum = CasinoBuilder.getTotalNum(areaAdd, casinoMachineInfo, cfg, timeMillis);
                    if (totalNum == 0) {
                        continue;
                    }
                    getReward.merge(cfg.getOutput().getFirst(), totalNum, Long::sum);
                    //修改数据
                    casinoMachineInfo.setProfitStartTime(timeMillis);
                    casinoSimpleInfos.add(CasinoBuilder.buildCasinoSimpleMachineInfo(casinoInfo, casinoMachineInfo, timeMillis));
                }
                //发奖
                CommonResult<PlayerPack> addItems = playerPackService.addItems(playerId, getReward, "一键领取赌场收益");
                if (!addItems.success()) {
                    res.code = Code.UNKNOWN_ERROR;
                    return res;
                }
            }
            res.itemInfos = new ArrayList<>();
            for (Map.Entry<Integer, Long> entry : getReward.entrySet()) {
                ItemInfo itemInfo = new ItemInfo();
                itemInfo.count = entry.getValue();
                itemInfo.itemId = entry.getKey();
                res.itemInfos.add(itemInfo);
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
            CasinoInfo casinoInfo = getCasinoInfo(playerId);
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            res.machineId = 0;
            List<CasinoSimpleInfo> casinoSimpleInfos = new ArrayList<>();
            res.casinoSimpleInfos = casinoSimpleInfos;
            CasinoMachineInfo casinoMachineInfo = casinoInfo.getMachineInfoData().get(req.machineId);
            if (Objects.isNull(casinoMachineInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            long timeMillis = System.currentTimeMillis();
            BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
            //计算总收获
//                long totalNum = CasinoBuilder.getTotalNum(, casinoMachineInfo, cfg, timeMillis);
            long totalNum = 0;
            casinoMachineInfo.setProfitStartTime(timeMillis);
            //发奖
            Item item = new Item(cfg.getOutput().getFirst(), totalNum);
            CommonResult<PlayerPack> addItem = playerPackService.addItem(playerId, item.getId(), item.getCount(), "一键领取机台收益");
            if (!addItem.success()) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            //TODO
            casinoSimpleInfos.add(CasinoBuilder.buildCasinoSimpleMachineInfo(casinoInfo, casinoMachineInfo, timeMillis));
            res.itemInfos = new ArrayList<>();
            res.itemInfos.add(CasinoBuilder.buildItemInfo(item));
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
     * @param playerId 玩家id
     * @param req      请求
     * @return 响应
     */
    public ResCasinoEmployStaff reqCasinoEmployStaff(long playerId, ReqCasinoEmployStaff req) {
        ResCasinoEmployStaff res = new ResCasinoEmployStaff();
        try {
            CasinoInfo casinoInfo = getCasinoInfo(playerId);
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            CasinoMachineInfo casinoMachineInfo = casinoInfo.getMachineInfoData().get(req.machineId);
            if (Objects.isNull(casinoMachineInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            DealerFunctionCfg dealerFunctionCfg = GameDataManager.getDealerFunctionCfg(req.staffId);
            if (Objects.isNull(dealerFunctionCfg)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            List<Integer> cost = dealerFunctionCfg.getHiringExpenses();
            Map<Integer, CasinoEmployment> employmentMap = casinoMachineInfo.getEmploymentMap();
            if (Objects.isNull(employmentMap)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            CasinoEmployment casinoEmployment = employmentMap.getOrDefault(req.index, new CasinoEmployment());
            long timeMillis = System.currentTimeMillis();
            //已经购买还没到结束时间
            if (casinoEmployment.getEmploymentEndTime() > timeMillis) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            Item costItem = new Item(cost.getFirst(), cost.getLast());
            CommonResult<PlayerPack> removed = playerPackService.removeItem(playerId, costItem, "请求雇员职员");
            if (!removed.success()) {
                res.code = Code.NOT_ENOUGH_ITEM;
                return res;
            }
            employmentMap.put(req.index, casinoEmployment);
            //设置数据
            casinoEmployment.setEmploymentId(req.staffId);
            casinoEmployment.setEmploymentEndTime(timeMillis + dealerFunctionCfg.getDuration());
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
     * @param player 玩家id
     * @param req    请求
     * @return 响应
     */
    public ResCasinoFloorOperation reqCasinoFloorOperation(Player player, ReqCasinoFloorOperation req) {
        ResCasinoFloorOperation res = new ResCasinoFloorOperation();
        try {
            CasinoInfo casinoInfo = getCasinoInfo(player.getId());
            if (Objects.isNull(casinoInfo)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            switch (req.type) {
                case 2 -> {
                    return cleanFloor(req, res, casinoInfo);
                }
                case 3 -> {
                    return overClean(player.getId(), req, res, casinoInfo);
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

    private ResCasinoFloorOperation overClean(long player, ReqCasinoFloorOperation req, ResCasinoFloorOperation res, CasinoInfo casinoInfo) {
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
        ItemInfo itemInfo = CasinoBuilder.calculateCostItemInfo(buildingCleaningEndTime, timeMillis);
        if (Objects.isNull(itemInfo)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //扣除消耗
        CommonResult<PlayerPack> removed = playerPackService.removeItem(player, itemInfo.itemId, itemInfo.count, "加速清理");
        if (!removed.success()) {
            res.code = removed.code;
            return res;
        }
        casinoInfo.getBuildingCleaningEndTime().put(req.floorId, timeMillis);
        res.casinoFloorInfo = CasinoBuilder.buildCasinoFloorInfo(casinoInfo, req.floorId, timeMillis, true);
        res.casinoId = req.casinoId;
        res.type = req.type;
        return res;
    }

    private ResCasinoFloorOperation cleanFloor(ReqCasinoFloorOperation req, ResCasinoFloorOperation res, CasinoInfo casinoInfo) {
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
        res.casinoFloorInfo = CasinoBuilder.buildCasinoFloorInfo(casinoInfo, req.floorId, System.currentTimeMillis(), false);
        res.casinoId = req.casinoId;
        res.type = req.type;
        return res;
    }


    public ResCasinoUpgradeMachine reqCasinoUpgradeMachine(Player player, ReqCasinoUpgradeMachine req) {
        ResCasinoUpgradeMachine res = new ResCasinoUpgradeMachine();
        try {
            PlayerBuilding playerBuilding = dataMap.get(player.getId());
            if (Objects.isNull(playerBuilding)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
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
                    return quickUpgrade(player, req, res, casinoInfo);
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

    private ResCasinoUpgradeMachine lvUp(Player player, ReqCasinoUpgradeMachine req, ResCasinoUpgradeMachine res, CasinoInfo casinoInfo) {
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
        //检查消耗
        if (!playerPackService.checkHasItems(player, functionCfg.getUplevel_itemid())) {
            res.code = Code.NOT_ENOUGH_ITEM;
            return res;
        }
        //扣除消耗
        CommonResult<PlayerPack> removed = playerPackService.removeItems(playerId, functionCfg.getUplevel_itemid(), "升级建筑");
        if (!removed.success()) {
            res.code = Code.NOT_ENOUGH_ITEM;
            return res;
        }
        //修改数据
        casinoMachineInfo.setLastConfigId(functionCfg.getId());
        casinoMachineInfo.setConfigId(nextCfg.getId());
        casinoMachineInfo.setBuildLvUpStartTime(timeMillis);
        casinoMachineInfo.setBuildLvUpEndTime(timeMillis + functionCfg.getUptime() * 1000L);
        //构建响应
        res.type = req.type;
        res.buildLvUpEndTime = casinoMachineInfo.getBuildLvUpEndTime();
        res.configId = casinoMachineInfo.getRealConfigId(timeMillis);
        res.machineId = casinoMachineInfo.getId();
        return res;
    }


    private ResCasinoUpgradeMachine quickUpgrade(Player player, ReqCasinoUpgradeMachine req, ResCasinoUpgradeMachine res, CasinoInfo casinoInfo) {
        long playerId = player.getId();
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
        ItemInfo itemInfo = CasinoBuilder.calculateCostItemInfo(buildLvUpEndTime, timeMillis);
        if (Objects.isNull(itemInfo)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //扣除消耗
        CommonResult<PlayerPack> removed = playerPackService.removeItem(playerId, itemInfo.itemId, itemInfo.count, "加速升级");
        if (!removed.success()) {
            res.code = Code.NOT_ENOUGH_ITEM;
            return res;
        }
        casinoMachineInfo.setBuildLvUpEndTime(timeMillis);
        if (casinoMachineInfo.getProfitStartTime() == 0) {
            BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
            if (Objects.nonNull(cfg) && CollectionUtil.isNotEmpty(cfg.getOutput())) {
                casinoMachineInfo.setProfitStartTime(timeMillis);
            }
        }
        //构建响应
        res.type = req.type;
        res.buildLvUpEndTime = timeMillis;
        res.configId = casinoMachineInfo.getRealConfigId(timeMillis);
        res.machineId = req.machineId;
        return res;
    }

    @Override
    public void onTimer(TimerEvent<String> e) {
        long timeMillis = System.currentTimeMillis();
        //定时回存
        if (e == casinoSave) {

        }
        if (e == casinoCheck) {
            for (PlayerBuilding playerBuilding : dataMap.values()) {
                CasinoInfo casinoInfo = playerBuilding.getCasinoInfo();
                Set<Long> changeMachineIds = new HashSet<>();
                Map<Long, CasinoMachineInfo> machineInfoData = casinoInfo.getMachineInfoData();
                for (Map.Entry<Integer, Long> entry : casinoInfo.getBuildingCleaningEndTime().entrySet()) {
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
                    List<CasinoSimpleInfo> simpleInfos = new ArrayList<>();
                    for (Long changeMachineId : changeMachineIds) {
                        CasinoMachineInfo casinoMachineInfo = machineInfoData.get(changeMachineId);
                        //如果收益开始时间为0设置为当前时间
                        if (casinoMachineInfo.getProfitStartTime() == 0) {
                            BuildingFunctionCfg cfg = GameDataManager.getBuildingFunctionCfg(casinoMachineInfo.getRealConfigId(timeMillis));
                            if (Objects.nonNull(cfg) && CollectionUtil.isNotEmpty(cfg.getOutput())) {
                                casinoMachineInfo.setProfitStartTime(timeMillis);
                            }
                        }
                        CasinoSimpleInfo casinoSimpleInfo = CasinoBuilder.buildCasinoSimpleMachineInfo(casinoInfo, casinoMachineInfo, timeMillis);
                        simpleInfos.add(casinoSimpleInfo);
                    }
                    NotifyCasinoSimpleChange notifyCasinoSimpleChange = new NotifyCasinoSimpleChange();
                    notifyCasinoSimpleChange.infos = simpleInfos;
                    playerController.send(notifyCasinoSimpleChange);
                }
            }
        }
    }


    @Override
    public void sessionClose(PFSession session) {
        if (session.getPlayerId() > 0) {
            PlayerBuilding playerBuilding = dataMap.get(session.getPlayerId());
            if (Objects.nonNull(playerBuilding)) {
                try {
                    log.info("退出回存我的赌场信息 playerId:{} ", session.playerId);
                    playerBuildingService.redisSave(playerBuilding.getPlayerId(), playerBuilding.getCasinoId(), playerBuilding);
                } catch (Exception e) {
                    log.error("退出时保存我的赌场数据失败 playerId:{} data:{}", session.playerId, playerBuilding);
                }
            }
            dataMap.remove(session.getPlayerId());
            playerControllerMap.remove(session.playerId);
        }
    }
}
