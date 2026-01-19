package com.jjg.game.activity.wealthroulette.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.activity.activitylog.ActivityLogger;
import com.jjg.game.activity.common.dao.RecordDao;
import com.jjg.game.activity.wealthroulette.dao.WealthRouletteDao;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteDrawInfo;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteDrawItemInfo;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteGoodInfo;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteHistoryInfo;
import com.jjg.game.activity.wealthroulette.message.req.ReqWealthRouletteBuyGood;
import com.jjg.game.activity.wealthroulette.message.req.ReqWealthRouletteDraw;
import com.jjg.game.activity.wealthroulette.message.req.ReqWealthRouletteHistory;
import com.jjg.game.activity.wealthroulette.message.res.*;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.base.gameevent.ClockEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.RouletteShopCfg;
import com.jjg.game.sampledata.bean.WealthRouletteRewardCfg;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 财富轮盘
 *
 * @author lm
 * @date 2025/12/1 09:37
 */
@Component
public class WealthRouletteController implements ConfigExcelChangeListener, IPlayerLoginSuccess, GameEventListener, IRedDotService {
    private final Logger log = LoggerFactory.getLogger(WealthRouletteController.class);
    private final CountDao countDao;
    private final GameFunctionService gameFunctionService;
    private final String PREFIX = "wealthroulette";
    private final String CURRENT_POINT = "now:%s";
    private final PlayerPackService playerPackService;
    private final WealthRouletteDao wealthRouletteDao;
    private final ClusterSystem clusterSystem;
    private final RedDotManager redDotManager;
    private final ActivityLogger activityLogger;
    private final RecordDao recordDao;
    private Map<Integer, RouletteShopCfg> dataCache = new HashMap<>();

    public WealthRouletteController(CountDao countDao, GameFunctionService gameFunctionService, PlayerPackService playerPackService,
                                    WealthRouletteDao wealthRouletteDao, ClusterSystem clusterSystem, RedDotManager redDotManager, ActivityLogger activityLogger, RecordDao recordDao) {
        this.countDao = countDao;
        this.gameFunctionService = gameFunctionService;
        this.playerPackService = playerPackService;
        this.wealthRouletteDao = wealthRouletteDao;
        this.clusterSystem = clusterSystem;
        this.redDotManager = redDotManager;
        this.activityLogger = activityLogger;
        this.recordDao = recordDao;
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(RouletteShopCfg.EXCEL_NAME, this::initCacheData)
                .addChangeSampleFileObserveWithCallBack(RouletteShopCfg.EXCEL_NAME, this::initCacheData);
    }

    public void initCacheData() {
        List<RouletteShopCfg> rouletteShopCfgList = GameDataManager.getRouletteShopCfgList();
        if (CollectionUtil.isEmpty(rouletteShopCfgList)) {
            return;
        }
        Map<Integer, RouletteShopCfg> map = new HashMap<>();
        for (RouletteShopCfg cfg : rouletteShopCfgList) {
            if (cfg.getOpen() <= 0) {
                continue;
            }
            map.put(cfg.getId(), cfg);
        }
        dataCache = map;
    }

    /**
     * 添加进度
     *
     * @param player   玩家数据
     * @param progress 进度
     */
    public void addProgress(Player player, int gameType, long progress) {
        if (isClose(player) || progress == 0) {
            return;
        }
        if (!canAddProgress(gameType)) {
            return;
        }
        //按游戏判断
        countDao.incrementWithoutExpireRefresh(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX), getChildId(player.getId(), LocalDate.now()),
                BigDecimal.valueOf(Math.abs(progress)), TimeHelper.DAY_SECOND * 2);
    }

    /**
     * 是否能增加进度
     *
     * @param gameType 游戏类型
     */
    private boolean canAddProgress(int gameType) {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(72);
        try {
            if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
                int majorTypeByGameType = CommonUtil.getMajorTypeByGameType(gameType);
                String[] split = StringUtils.split(globalConfigCfg.getValue(), "|");
                for (String gameCfg : split) {
                    String[] cfg = StringUtils.split(gameCfg, "_");
                    if (cfg.length < 2) {
                        continue;
                    }
                    int majorType = Integer.parseInt(cfg[0]);
                    if (majorTypeByGameType != majorType) {
                        continue;
                    }
                    for (int i = 1; i < cfg.length; i++) {
                        int needGameId = Integer.parseInt(cfg[i]);
                        if (needGameId == -1 || needGameId == gameType) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("财富轮盘 全局表配置错误  gameType:{}", gameType, e);
        }
        return false;
    }

    /**
     * 获取转换后的积分
     *
     * @param playerId        玩家id
     * @param conversionValue 需要转换值
     */
    private BigDecimal getConversionValue(long playerId, long conversionValue, boolean realConversion) {
        //获取转换率
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(70);
        if (globalConfigCfg == null || StringUtils.isEmpty(globalConfigCfg.getValue())) {
            log.error("财富轮盘 全局表配置错误 playerId:{} addValue:{}", playerId, conversionValue);
            return BigDecimal.ZERO;
        }
        //第一位金币 第二位积分1000000_1
        String[] cfg = StringUtils.split(globalConfigCfg.getValue(), "_");
        if (cfg.length != 2) {
            log.error("财富轮盘 全局表配置错误cfg playerId:{} addValue:{}", playerId, conversionValue);
            return BigDecimal.ZERO;
        }
        int gold = 0;
        int point = 0;
        try {
            gold = Integer.parseInt(cfg[0]);
            point = Integer.parseInt(cfg[1]);
        } catch (Exception e) {
            log.error("财富轮盘 全局表配置错误numberCast playerId:{} addValue:{}", playerId, conversionValue, e);
        }
        if (gold == 0 || point == 0) {
            log.error("财富轮盘 全局表配置错误gold or point playerId:{} addValue:{} ", playerId, conversionValue);
            return BigDecimal.ZERO;
        }
        BigDecimal multiplied = BigDecimal.valueOf(conversionValue).multiply(BigDecimal.valueOf(point));
        if (realConversion) {
            return multiplied.divide(BigDecimal.valueOf(gold), RoundingMode.DOWN);
        }
        return multiplied.divide(BigDecimal.valueOf(gold), 4, RoundingMode.DOWN);
    }

    /**
     * 获取子id
     *
     * @return 按日期的子id
     */
    private String getChildId(long playerId, LocalDate localDate) {
        return playerId + localDate.format(TimeHelper.FORMATTER);
    }


    /**
     * 是否开放
     *
     * @param player 玩家数据
     * @return 是否开放
     */
    public boolean isClose(Player player) {
        return !gameFunctionService.checkGameFunctionOpen(player, EFunctionType.WEALTH_ROULETTE);
    }

    public AbstractResponse reqWealthRouletteDetailInfo(Player player) {
        long playerId = player.getId();
        ResWealthRouletteDetailInfo res = new ResWealthRouletteDetailInfo(Code.SUCCESS);
        if (isClose(player)) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        res.currentPoint = countDao.getCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX), CURRENT_POINT.formatted(playerId)).intValue();
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(71);
        if (globalConfigCfg != null) {
            res.drawNeedPoint = globalConfigCfg.getIntValue();
        }
        res.rouletteItemInfo = new ArrayList<>();
        for (WealthRouletteRewardCfg rewardCfg : GameDataManager.getWealthRouletteRewardCfgList()) {
            res.rouletteItemInfo.add(new WealthRouletteDrawItemInfo(rewardCfg.getId(), rewardCfg.getPicture(), ItemUtils.buildItemInfo(rewardCfg.getItem())));
        }
        res.totalPoint = getTodayMaxPoint(playerId);
        BigDecimal current = countDao.getCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX),
                getChildId(playerId, LocalDate.now()));
        res.tomorrowPoint = getConversionValue(playerId, current.longValue(), false).toPlainString();
        return res;
    }

    /**
     * 获取今日最大积分
     *
     * @param playerId 玩家id
     * @return 积分
     */
    private int getTodayMaxPoint(long playerId) {
        long point = countDao.getCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX),
                getChildId(playerId, LocalDate.now().minusDays(1))).longValue();
        return getConversionValue(playerId, point, true).intValue();
    }

    /**
     * 抽奖
     *
     * @param player 玩家信息
     */
    public AbstractResponse reqWealthRouletteDraw(Player player, ReqWealthRouletteDraw req) {
        ResWealthRouletteDraw res = new ResWealthRouletteDraw(Code.SUCCESS);
        long playerId = player.getId();
        if (isClose(player)) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        //获取配置的每次所需积分
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(71);
        if (cfg == null || cfg.getIntValue() <= 0) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        //获取奖励配置信息
        List<WealthRouletteRewardCfg> cfgList = GameDataManager.getWealthRouletteRewardCfgList();
        if (CollectionUtil.isEmpty(cfgList)) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        //获取当日积分
        BigDecimal count = countDao.getCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX), CURRENT_POINT.formatted(playerId));
        int times = req.times == -1 ? Math.min(100, count.intValue() / cfg.getIntValue()) : req.times;
        if (times <= 0) {
            res.code = Code.WEALTH_ROULETTE_NOT_POINT;
            return res;
        }
        int needPoint = times * cfg.getIntValue();
        //封装权重随机
        WeightRandom<WealthRouletteRewardCfg> random = new WeightRandom<>();
        for (WealthRouletteRewardCfg rewardCfg : cfgList) {
            if (CollectionUtil.isEmpty(rewardCfg.getItem())) {
                continue;
            }
            random.add(rewardCfg, rewardCfg.getWeight());
        }
        //奖励信息
        Map<Integer, Integer> rewardMap = new HashMap<>();
        Map<Integer, Long> finalRewardMap = new HashMap<>();
        for (int i = 0; i < times; i++) {
            WealthRouletteRewardCfg next = random.next();
            if (next == null) {
                res.code = Code.SAMPLE_ERROR;
                return res;
            }
            rewardMap.merge(next.getId(), 1, Integer::sum);
            ItemUtils.mergeItems(finalRewardMap, next.getItem());
        }
        if (finalRewardMap.isEmpty()) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        //扣除积分
        BigDecimal result = countDao.decrementIfSufficient(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX), CURRENT_POINT.formatted(playerId), BigDecimal.valueOf(needPoint));
        if (result == null) {
            res.code = Code.WEALTH_ROULETTE_NOT_POINT;
            return res;
        }
        //发送奖励
        CommonResult<ItemOperationResult> addItems = playerPackService.addItems(playerId, finalRewardMap, AddType.ACTIVITY_WEALTH_ROULETTE_REWARDS);
        if (!addItems.success()) {
            log.error("玩家添加财富转盘奖励失败 playerId:{} addItems:{}", playerId, finalRewardMap);
        }
        //发送日志
        int toDayMax = getTodayMaxPoint(playerId);
        activityLogger.sendWealthRouletteLog(player, toDayMax, needPoint, result.intValue(), 1, finalRewardMap, addItems);
        addHistoryRecord(playerId, 1, needPoint, finalRewardMap);
        //构建响应信息
        res.remainPoint = result.intValue();
        res.drawInfos = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : rewardMap.entrySet()) {
            WealthRouletteDrawInfo drawInfo = new WealthRouletteDrawInfo();
            WealthRouletteRewardCfg rewardCfg = GameDataManager.getWealthRouletteRewardCfgMap().get(entry.getKey());
            drawInfo.times = entry.getValue();
            drawInfo.configId = rewardCfg.getId();
            res.drawInfos.add(drawInfo);
        }
        if (result.intValue() < cfg.getIntValue()) {
            updateRedDot(playerId, 0);
        }
        return res;
    }

    public AbstractResponse reqWealthRouletteBuyGood(Player player, ReqWealthRouletteBuyGood req) {
        ResWealthRouletteBuyGood res = new ResWealthRouletteBuyGood(Code.SUCCESS);
        long playerId = player.getId();
        if (isClose(player) || req.buyNum <= 0) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        RouletteShopCfg cfg = dataCache.get(req.goodId);
        if (cfg == null || cfg.getPurchase() <= 0 || CollectionUtil.isEmpty(cfg.getItem())) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        //购买商品
        RMap<Integer, Integer> playerBuyTimes = wealthRouletteDao.getPlayerBuyTimes(playerId);
        int buyTimes = playerBuyTimes.getOrDefault(req.goodId, 0);
        boolean noLimit = cfg.getFrequency() != -1;
        if (noLimit && buyTimes + req.buyNum > cfg.getFrequency()) {
            res.code = Code.WEALTH_ROULETTE_BUY_LIMIT;
            return res;
        }
        int needPoint = cfg.getPurchase() * req.buyNum;
        //扣除积分
        BigDecimal remainPoint = countDao.decrementIfSufficient(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX), CURRENT_POINT.formatted(playerId),
                BigDecimal.valueOf(needPoint));
        if (remainPoint == null) {
            res.code = Code.WEALTH_ROULETTE_NOT_POINT;
            return res;
        }
        Long result = -1L;
        if (noLimit) {
            //增加限购次数
            result = wealthRouletteDao.incrementIfLessThan(playerId, req.goodId, req.buyNum, cfg.getFrequency());
            if (result == null) {
                res.code = Code.WEALTH_ROULETTE_BUY_LIMIT;
                countDao.incrBy(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX), getChildId(playerId, LocalDate.now()),
                        BigDecimal.valueOf(needPoint));
                return res;
            }
        }
        //发送奖励
        Map<Integer, Long> addItemMap = ItemUtils.expendItems(cfg.getItem(), req.buyNum);
        CommonResult<ItemOperationResult> addResult = playerPackService.addItems(playerId, addItemMap, AddType.ACTIVITY_WEALTH_ROULETTE_REWARDS);
        if (!addResult.success()) {
            log.error("财富转盘 购买商品后添加道具失败 playerId:{} goodId:{}", playerId, req.goodId);
        }
        //发送日志
        int toDayMax = getTodayMaxPoint(playerId);
        activityLogger.sendWealthRouletteLog(player, toDayMax, needPoint, remainPoint.intValue(), 2, addItemMap, addResult);
        addHistoryRecord(playerId, 2, needPoint, addItemMap);
        res.goodInfo = buildWealthRouletteGoodInfo(cfg, result.intValue());
        res.remainPoint = remainPoint.intValue();
        res.buyNum = req.buyNum;
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(71);
        if (globalConfigCfg != null && globalConfigCfg.getIntValue() > 0) {
            if (remainPoint.intValue() < globalConfigCfg.getIntValue()) {
                updateRedDot(playerId, 0);
            }
        }
        return res;
    }

    /**
     * 添加历史记录
     *
     * @param playerId   玩家id
     * @param cost       花费积分
     * @param type       类型 1旋转 2兑换
     * @param addItemMap 奖励道具
     */
    private void addHistoryRecord(long playerId, int type, int cost, Map<Integer, Long> addItemMap) {
        //记录玩家日志
        try {
            WealthRouletteHistoryInfo wealthRouletteHistoryInfo = new WealthRouletteHistoryInfo();
            wealthRouletteHistoryInfo.cost = cost;
            wealthRouletteHistoryInfo.date = System.currentTimeMillis();
            wealthRouletteHistoryInfo.type = type;
            wealthRouletteHistoryInfo.itemInfo = ItemUtils.buildItemInfo(addItemMap);
            recordDao.addRecord(PREFIX, 0, playerId, wealthRouletteHistoryInfo, 100, false);
        } catch (Exception e) {
            log.error("财富轮盘添加历史记录失败 playerId:{} cost:{} type:{} addItemMap:{}", playerId, cost, type, JSON.toJSON(addItemMap), e);
        }
    }

    public AbstractResponse reqWealthRouletteShopInfos(Player player) {
        ResWealthRouletteShopInfos res = new ResWealthRouletteShopInfos(Code.SUCCESS);
        if (isClose(player)) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        RMap<Integer, Integer> playerBuyTimes = wealthRouletteDao.getPlayerBuyTimes(player.getId());
        Map<Integer, Integer> map = Map.of();
        if (playerBuyTimes != null) {
            map = playerBuyTimes.getAll(dataCache.keySet());
        }
        res.shopInfos = new ArrayList<>();
        for (RouletteShopCfg shopCfg : dataCache.values()) {
            if (shopCfg.getOpen() <= 0) {
                continue;
            }
            res.shopInfos.add(buildWealthRouletteGoodInfo(shopCfg, map.getOrDefault(shopCfg.getId(), 0)));
        }
        return res;
    }

    public WealthRouletteGoodInfo buildWealthRouletteGoodInfo(RouletteShopCfg cfg, int buyTimes) {
        WealthRouletteGoodInfo goodInfo = new WealthRouletteGoodInfo();
        goodInfo.itemInfos = ItemUtils.buildItemInfo(cfg.getItem());
        goodInfo.id = cfg.getId();
        goodInfo.needPoint = cfg.getPurchase();
        goodInfo.maxBuyTimes = cfg.getFrequency();
        goodInfo.sort = cfg.getSequence();
        goodInfo.currentBuyTimes = buyTimes;
        return goodInfo;
    }

    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, Account account, boolean firstLogin) {
        if (firstLogin) {
            if (isClose(player)) {
                return;
            }
            resetData(player.getId());
        }
    }

    /**
     * 重置数据
     *
     * @param playerId 玩家id
     */
    private void resetData(long playerId) {
        if (!wealthRouletteDao.canTargetFirstLogin(playerId)) {
            log.error("财富转盘 已经触发过首次登陆了 playerId:{}", playerId);
            return;
        }
        //清除数据
        wealthRouletteDao.getPlayerBuyTimes(playerId).delete();
        //计算当天积分
        BigDecimal count = countDao.getCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX),
                getChildId(playerId, LocalDate.now().minusDays(1)));
        BigDecimal add = getConversionValue(playerId, count.longValue(), true);
        if (add.compareTo(BigDecimal.ZERO) >= 0) {
            countDao.setCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX), CURRENT_POINT.formatted(playerId), add);
            log.info("财富转盘 今日积分 playerOd:{} addPoint:{}", playerId, add.longValue());
        }
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof ClockEvent clockEvent && clockEvent.getHour() == 0) {
            if (!gameFunctionService.checkGameFunctionOpen(EFunctionType.WEALTH_ROULETTE)) {
                return;
            }
            //获取所有在线玩家
            List<PFSession> allOnlinePlayerPFSession = clusterSystem.getAllOnlinePlayerPFSession();
            for (PFSession pfSession : allOnlinePlayerPFSession) {
                long playerId = pfSession.playerId;
                if (playerId <= 0 || !(pfSession.getReference() instanceof PlayerController playerController)) {
                    continue;
                }
                Player player = playerController.getPlayer();
                if (player == null || isClose(player)) {
                    continue;
                }
                PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(pfSession.getWorkId(), 0, new BaseHandler<String>() {
                    @Override
                    public void action() {
                        resetData(playerId);
                    }
                });
            }
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.CLOCK_EVENT);
    }

    /**
     * 更新红点
     */
    public void updateRedDot(long playerId, int redCount) {
        redDotManager.updateRedDot(getModule(), getSubmodule(), playerId, redCount);
    }

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.ACTIVITY;
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        BigDecimal count = countDao.getCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(PREFIX), CURRENT_POINT.formatted(playerId));
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(71);
        int redCount = 0;
        if (globalConfigCfg != null && globalConfigCfg.getIntValue() > 0) {
            if (count.longValue() >= globalConfigCfg.getIntValue()) {
                redCount = 1;
            }
        }
        return List.of(redDotManager.buildRedDotDetails(RedDotDetails.RedDotModule.ACTIVITY, getSubmodule(), redCount));
    }

    @Override
    public int getSubmodule() {
        return 999;
    }

    /**
     * 请求财富轮盘历史记录
     *
     * @param player 玩家数据
     * @param req    请求参数
     * @return 响应结果
     */
    public AbstractResponse reqWealthRouletteHistory(Player player, ReqWealthRouletteHistory req) {
        ResWealthRouletteHistory res = new ResWealthRouletteHistory(Code.SUCCESS);
        if (isClose(player)) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        // 查询玩家或全局中奖记录（分页）
        Pair<Boolean, List<WealthRouletteHistoryInfo>> historyRecodes = recordDao.getPlayerRecords(PREFIX, 0, player.getId(),
                req.startIndex, req.size, WealthRouletteHistoryInfo.class);
        res.hasNext = historyRecodes.getFirst();
        if (req.startIndex == 0) {
            res.totalCount = recordDao.getPlayerRecordCount(PREFIX, 0, player.getId());
        }
        List<WealthRouletteHistoryInfo> historyRecords = historyRecodes.getSecond();
        if (CollectionUtil.isNotEmpty(historyRecords)) {
            res.wealthRouletteHistoryInfos = historyRecords;
        }
        res.startIndex = req.startIndex;
        return res;
    }
}
