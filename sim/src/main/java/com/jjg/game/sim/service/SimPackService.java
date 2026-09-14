package com.jjg.game.sim.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.listener.SpecialItemListener;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.season.dao.SeasonPlayerDao;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.service.SeasonEconomyService;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimSpecialItemBalanceListener;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * sim 特殊资源（能量、知名度、曝光度、场景经验、赛季币、勋章）的承载实现。
 * <p>
 * 这些资源不存在背包里：玩家在本节点在线时改内存态（随 ctx 定时落库），不在本节点时直写持久化数据。
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class SimPackService implements SpecialItemListener {
    private static final Logger log = LoggerFactory.getLogger(SimPackService.class);

    @Autowired
    private SimPlayerGameDao simPlayerGameDao;
    @Autowired
    private SimCasinoDao simCasinoDao;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;
    @Lazy
    @Autowired
    private SimCasinoService simCasinoService;
    @Lazy
    @Autowired
    private SimGuestService simGuestService;
    @Lazy
    @Autowired
    private SimEmployeeService simEmployeeService;
    @Autowired
    private SeasonPlayerDao seasonPlayerDao;
    @Autowired
    private SimNodeService simNodeService;
    @Autowired
    private CoreLogger coreLogger;
    @Autowired
    private RedDotManager redDotManager;
    @Autowired
    private ClusterSystem clusterSystem;
    @ClusterRpcReference
    private ToSimBridge toSimBridge;
    //@Lazy 打破循环: 本类 -> SeasonEconomyService -> PlayerPackService -> 本类
    @Lazy
    @Autowired
    private SeasonEconomyService seasonEconomyService;
    @Autowired(required = false)
    private List<SimSpecialItemBalanceListener> balanceListeners = List.of();

    @Override
    public long getItemCount(long playerId, int itemId) {
        ClusterClient owner = findRemoteSimNode(playerId);
        if (owner != null) {
            return rpcCall(owner, playerId, () -> toSimBridge.getSimItemCount(playerId, itemId), 0L, "读取");
        }
        return getItemCountHere(playerId, itemId);
    }

    /**
     * 在本节点读取（跨节点转发的落点）：不再二次路由。
     */
    public long getItemCountHere(long playerId, int itemId) {
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        if (itemId == SimConstant.Item.ID_SEASON_COIN) {
            SeasonPlayerData seasonData = ctx == null ? null : ctx.getSeasonPlayerData();
            if (seasonData != null) {
                return seasonData.getSeasonCoin();
            }
            return seasonPlayerDao.findById(playerId).map(SeasonPlayerData::getSeasonCoin).orElse(0L);
        }
        if (itemId == SimConstant.Item.ID_POWER) {
            SimBaseData base = getBaseData(playerId, ctx);
            return base == null ? 0 : base.getPower();
        }
        if (itemId == SimConstant.Item.ID_AWARENESS) {
            SimCasinoData casino = getCurrentCasino(playerId, ctx);
            return casino == null ? 0 : casino.getAwareness();
        }
        if (itemId == SimConstant.Item.CASINO_EXP) {
            SimCasinoData casino = getCurrentCasino(playerId, ctx);
            return casino == null ? 0 : casino.getExp();
        }
        if (GameDataManager.getMedalListCfg(itemId) != null) {
            SimBaseData base = getBaseData(playerId, ctx);
            Set<Integer> medalIds = base == null ? null : base.getAllMedalIds();
            return medalIds != null && medalIds.contains(itemId) ? 1 : 0;
        }
        //曝光度无承载
        return 0;
    }

    @Override
    public boolean addItems(long playerId, List<Item> items, AddType addType, String desc, boolean notify) {
        ClusterClient owner = findRemoteSimNode(playerId);
        Map<Integer, Long> addedItems = mergeItems(items, false);
        Map<Integer, Long> beforeBalances = getCurrentBalances(owner, playerId, addedItems.keySet());
        boolean added;
        if (owner != null) {
            added = rpcCall(owner, playerId,
                    () -> toSimBridge.addSimItems(playerId, items, addType, desc), false, "入账");
        } else {
            added = addItemsHere(playerId, items, addType);
        }
        if (added) {
            if (!addedItems.isEmpty()) {
                Map<Integer, Long> afterBalances = getCurrentBalances(owner, playerId, addedItems.keySet());
                coreLogger.addItems(playerId, beforeBalances, addedItems, afterBalances, addType, desc);
            }
            notifyCurrentBalances(owner, playerId, items);
        }
        return added;
    }

    /**
     * 在本节点入账（跨节点转发的落点）：不再二次路由，避免节点间来回转发。
     */
    public boolean addItemsHere(long playerId, List<Item> items, AddType addType) {
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        return ctx != null ? addItemsOnline(ctx, items, addType) : addItemsOffline(playerId, items);
    }

    /**
     * 在线入账：改内存态，随 ctx 定时落库
     */
    private boolean addItemsOnline(SimPlayerContext ctx, List<Item> items, AddType addType) {
        boolean specialGuestItemAdded = false;
        for (Item item : items) {
            int itemId = item.getId();
            long count = item.getItemCount();
            if (count <= 0) {
                continue;
            }

            ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
            if (itemCfg == null) {
                continue;
            }

            if (itemId == SimConstant.Item.ID_POWER) {  //能量
                SimBaseData base = ctx.getSimBaseData();
                base.setPower(base.getPower() + (int) count);
            } else if (itemId == SimConstant.Item.ID_AWARENESS) {  //知名度
                SimCasinoData casino = ctx.getCurrentCasino();
                if (casino != null) {
                    casino.setAwareness(casino.getAwareness() + (int) count);
                }
            } else if (itemId == SimConstant.Item.CASINO_EXP) {  //场景经验
                simCasinoService.addCasinoExp(ctx, count);
            } else if (itemId == SimConstant.Item.ID_EXPOD) {  //曝光度无承载, 丢弃
                continue;
            } else if (GameDataManager.getMedalListCfg(itemId) != null) {  //勋章
                ctx.getSimBaseData().activeMedalId(itemId);
            } else if (itemId == SimConstant.Item.ID_SEASON_COIN) {  //赛季币
                addSeasonCoin(ctx, count);
            } else if (itemCfg.getItemType() == GameConstant.Item.ITEM_TYPE_SIM_RECRUIT_CARD) {  //招商卡
                ctx.getCurrentCasino().addSpecialGuest(itemId, count);
                specialGuestItemAdded = true;
            } else if (itemCfg.getItemType() == GameConstant.Item.ITEM_TYPE_SIM_GUEST) { //游客
                if (!simGuestService.addGuestItem(ctx, itemId, count, addType)) return false;
            } else if (itemCfg.getItemType() == GameConstant.Item.ITEM_TYPE_SIM_EMPLOYEE) {  //雇员
                if (!simEmployeeService.addEmployeeItem(ctx, itemId, count, addType)) return false;
            }
        }
        if (specialGuestItemAdded) {
            redDotManager.updateRedDotByInitialize(RedDotDetails.RedDotModule.SPECIAL_GUEST,
                    SimConstant.SpecialGuest.RED_DOT_INVITE_ITEM, ctx.playerId());
        }
        return true;
    }

    /**
     * 在线赛季币入账：仅增加可用余额，累计获得量只由赛季匹配获胜推进。
     */
    private void addSeasonCoin(SimPlayerContext ctx, long count) {
        if (ctx.getSeasonPlayerData() == null) {
            log.warn("赛季币入账失败, 无SeasonPlayerData playerId={},count={}", ctx.playerId(), count);
            return;
        }
        seasonEconomyService.addBalance(ctx.getSeasonPlayerData(), count);
    }

    /**
     * 离线（玩家不在本节点）入账：直接写入持久化数据。
     * <p>
     * 先把要写的载体全部取到，任一缺失就整体失败且不落任何一笔 —— 否则调用方重试会重复入账。
     * 注: 若玩家此刻正在其它节点在线, 该节点的内存快照落库可能覆盖此处直写。
     */
    private boolean addItemsOffline(long playerId, List<Item> items) {
        int powerAdd = 0;
        int awarenessAdd = 0;
        long seasonCoinAdd = 0;
        int casinoExpAdd = 0;

        List<Integer> medalIds = new ArrayList<>();
        for (Item item : items) {
            int itemId = item.getId();
            long count = item.getItemCount();
            if (count <= 0) {
                continue;
            }
            if (itemId == SimConstant.Item.CASINO_EXP) {
                casinoExpAdd += (int) count;
            }
            if (itemId == SimConstant.Item.ID_POWER) {
                powerAdd += (int) count;
            } else if (itemId == SimConstant.Item.ID_AWARENESS) {
                awarenessAdd += (int) count;
            } else if (itemId == SimConstant.Item.ID_SEASON_COIN) {
                seasonCoinAdd = Math.addExact(seasonCoinAdd, count);
            } else if (GameDataManager.getMedalListCfg(itemId) != null) {
                medalIds.add(itemId);
            }
            //曝光度无承载, 与在线入账一致丢弃
        }

        //能量与勋章存于 SimBaseData; 知名度存于当前场景 SimCasinoData; 赛季币存于 SeasonPlayerData
        boolean needBase = powerAdd > 0 || !medalIds.isEmpty() || casinoExpAdd > 0 || awarenessAdd > 0;
        SimBaseData base = needBase ? simPlayerGameDao.findById(playerId).orElse(null) : null;
        if (needBase && base == null) {
            log.warn("离线发放sim资源失败, 无SimBaseData playerId={},items={}", playerId, items);
            return false;
        }

        boolean needCasino = awarenessAdd > 0 || casinoExpAdd > 0;
        SimCasinoData casino = needCasino ? findOfflineCasino(playerId, base.getCurrentCasinoId()) : null;
        if (needCasino && casino == null) {
            log.warn("离线发放知名度或场景经验失败, 无场景 playerId={},awareness={},casinoExpAdd={}", playerId, awarenessAdd, casinoExpAdd);
            return false;
        }
        SeasonPlayerData seasonData = seasonCoinAdd > 0 ? seasonPlayerDao.findById(playerId).orElse(null) : null;
        if (seasonCoinAdd > 0 && seasonData == null) {
            log.warn("离线发放赛季币失败, 无SeasonPlayerData playerId={},count={}", playerId, seasonCoinAdd);
            return false;
        }

        if (needBase && (powerAdd > 0 || !medalIds.isEmpty())) {
            base.setPower(base.getPower() + powerAdd);
            medalIds.forEach(base::activeMedalId);
            simPlayerGameDao.save(base);
        }
        if (casino != null) {
            casino.setAwareness(casino.getAwareness() + awarenessAdd);
            casino.setExp(casino.getExp() + casinoExpAdd);
            simCasinoDao.save(casino);
        }
        if (seasonData != null) {
            seasonData.setSeasonCoin(Math.addExact(seasonData.getSeasonCoin(), seasonCoinAdd));
            seasonPlayerDao.save(seasonData);
        }
        return true;
    }

    /**
     * 离线取场景: 优先当前场景, currentCasinoId 失效则回退任一场景。
     */
    private SimCasinoData findOfflineCasino(long playerId, int currentCasinoId) {
        SimCasinoData casino = currentCasinoId > 0 ? simCasinoDao.findOne(playerId, currentCasinoId) : null;
        return casino == null ? simCasinoDao.findFirstByPlayerId(playerId) : casino;
    }

    /**
     * 找玩家 sim 会话所在的远端节点。
     * <p>
     * 本节点有会话（本地内存态最权威，不受路由记录过期影响）、无路由记录（真离线）、
     * 路由就指向本节点、路由指向的节点已不可达，这四种都返回 null，由调用方在本节点处理。
     */
    private ClusterClient findRemoteSimNode(long playerId) {
        if (simPlayerContextRegistry.getContext(playerId) != null) {
            return null;
        }
        String ownerPath = simNodeService.get(playerId);
        if (ownerPath == null || ownerPath.isEmpty() || ownerPath.equals(clusterSystem.getNodePath())) {
            return null;
        }
        ClusterClient client = clusterSystem.getClusterByPath(ownerPath);
        if (client == null) {
            log.warn("sim路由指向的节点不可用, 退化为本节点直写 playerId={},path={}", playerId, ownerPath);
        }
        return client;
    }

    /**
     * 把特殊资源的读写转发到玩家 sim 会话所在节点执行。
     *
     * @param failValue 调用失败时的返回值（写操作 false / 读操作 0）
     */
    private <T> T rpcCall(ClusterClient owner, long playerId, Supplier<CommonResult<T>> call, T failValue, String action) {
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previous = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(owner).setTryMillisPerClient(1000));
            CommonResult<T> result = call.get();
            if (result == null || !result.success() || result.data == null) {
                log.warn("跨节点{}sim特殊资源失败 playerId={},code={}",
                        action, playerId, result == null ? null : result.code);
                return failValue;
            }
            return result.data;
        } catch (Exception e) {
            log.error("跨节点{}sim特殊资源异常 playerId={}", action, playerId, e);
            return failValue;
        } finally {
            rpcContext.setReqParameterBuilder(previous);
        }
    }

    /**
     * 背包整笔入账成功后，把获得事件转发给持有 ctx 的 Hall。
     */
    public boolean forwardPackItemsAdded(long playerId, Map<Integer, Long> items, AddType addType) {
        ClusterClient owner = findRemoteSimNode(playerId);
        if (owner == null) {
            return false;
        }
        return rpcCall(owner, playerId,
                () -> toSimBridge.onPackItemsAdded(playerId, items, addType), false, "转发入账事件");
    }

    /**
     * 背包整笔扣除成功后，把消费事件转发给持有 ctx 的 Hall。
     */
    public boolean forwardPackItemsConsumed(long playerId, Map<Integer, Long> items, AddType addType) {
        ClusterClient owner = findRemoteSimNode(playerId);
        if (owner == null) {
            return false;
        }
        return rpcCall(owner, playerId,
                () -> toSimBridge.onPackItemsConsumed(playerId, items, addType), false, "转发消费事件");
    }

    private void notifyCurrentBalances(ClusterClient owner, long playerId, List<Item> items) {
        if (balanceListeners.isEmpty()) {
            return;
        }
        Set<Integer> notifiedItemIds = new HashSet<>();
        for (Item item : items) {
            int itemId = item.getId();
            if (item.getItemCount() <= 0 || !notifiedItemIds.add(itemId)) {
                continue;
            }
            boolean needed = false;
            for (SimSpecialItemBalanceListener listener : balanceListeners) {
                if (listener.support(itemId)) {
                    needed = true;
                    break;
                }
            }
            if (!needed) {
                continue;
            }
            Long balance = owner == null
                    ? getItemCountHere(playerId, itemId)
                    : rpcCall(owner, playerId,
                    () -> toSimBridge.getSimItemCount(playerId, itemId), null, "同步余额");
            if (balance == null) {
                continue;
            }
            for (SimSpecialItemBalanceListener listener : balanceListeners) {
                if (!listener.support(itemId)) {
                    continue;
                }
                try {
                    listener.onBalanceChanged(playerId, itemId, balance);
                } catch (Exception e) {
                    log.error("同步sim特殊资源余额监听器异常 listener={},playerId={},itemId={},balance={}",
                            listener.getClass().getSimpleName(), playerId, itemId, balance, e);
                }
            }
        }
    }

    /**
     * 扣除特殊资源（曝光度、场景经验与勋章无扣除承载）。
     * <p>
     * 与入账同样按 sim 会话所在节点路由；确实离线才直写持久化数据 —— 入账支持离线，
     * 扣除也必须支持，否则离线发的混合奖励一旦背包侧失败就补偿不回来。
     */
    @Override
    public boolean removeItems(long playerId, List<Item> items, AddType addType, String desc) {
        ClusterClient owner = findRemoteSimNode(playerId);
        Map<Integer, Long> removedItems = mergeItems(items, true);
        Map<Integer, Long> beforeBalances = getCurrentBalances(owner, playerId, removedItems.keySet());
        boolean removed;
        if (owner != null) {
            removed = rpcCall(owner, playerId,
                    () -> toSimBridge.removeSimItems(playerId, items, addType, desc), false, "扣除");
        } else {
            removed = removeItemsHere(playerId, items, addType);
        }
        if (removed) {
            if (!removedItems.isEmpty()) {
                Map<Integer, Long> afterBalances = getCurrentBalances(owner, playerId, removedItems.keySet());
                coreLogger.consumeItem(playerId, beforeBalances, removedItems, afterBalances, addType);
            }
            notifyCurrentBalances(owner, playerId, items);
        }
        return removed;
    }

    private Map<Integer, Long> mergeItems(List<Item> items, boolean removableOnly) {
        Map<Integer, Long> itemCounts = new HashMap<>();
        for (Item item : items) {
            int itemId = item.getId();
            if (item.getItemCount() <= 0 || removableOnly && !isRemovable(itemId)) {
                continue;
            }
            itemCounts.merge(itemId, item.getItemCount(), Long::sum);
        }
        return itemCounts;
    }

    private boolean isRemovable(int itemId) {
        return itemId == SimConstant.Item.ID_POWER
                || itemId == SimConstant.Item.ID_AWARENESS
                || itemId == SimConstant.Item.ID_SEASON_COIN;
    }

    private Map<Integer, Long> getCurrentBalances(ClusterClient owner, long playerId, Set<Integer> itemIds) {
        Map<Integer, Long> balances = new HashMap<>(itemIds.size());
        for (int itemId : itemIds) {
            Long balance = owner == null
                    ? getItemCountHere(playerId, itemId)
                    : rpcCall(owner, playerId,
                    () -> toSimBridge.getSimItemCount(playerId, itemId), null, "读取日志余额");
            if (balance != null) {
                balances.put(itemId, balance);
            }
        }
        return balances;
    }

    /**
     * 在本节点扣除（跨节点转发的落点）：不再二次路由，避免节点间来回转发。
     */
    public boolean removeItemsHere(long playerId, List<Item> items, AddType addType) {
        long needPower = 0;
        long needAwareness = 0;
        long needSeasonCoin = 0;
        for (Item item : items) {
            int itemId = item.getId();
            long count = item.getItemCount();
            if (count <= 0) {
                continue;
            }
            if (itemId == SimConstant.Item.ID_POWER) {
                needPower += count;
            } else if (itemId == SimConstant.Item.ID_AWARENESS) {
                needAwareness += count;
            } else if (itemId == SimConstant.Item.ID_SEASON_COIN) {
                needSeasonCoin += count;
            } else if (addType == AddType.FAIL_ROLLBACK) {
                //曝光度无承载、勋章激活后不可撤销；两者重复入账都是幂等的，
                //回滚时跳过即可，不能因此让同批的能量/赛季币也退不回去
                continue;
            } else {
                log.warn("扣除sim特殊资源失败, 该资源不支持扣除 playerId={},itemId={},count={}", playerId, itemId, count);
                return false;
            }
        }

        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            return removeItemsOffline(playerId, needPower, needAwareness, needSeasonCoin);
        }
        SimBaseData base = ctx.getSimBaseData();
        SimCasinoData casino = ctx.getCurrentCasino();
        SeasonPlayerData seasonData = ctx.getSeasonPlayerData();
        //先校验是否充足, 任一不足整体失败且不产生扣除
        if (needPower > 0 && base.getPower() < needPower) {
            log.warn("扣除道具失败, 能量不足 playerId={},need={},have={}", playerId, needPower, base.getPower());
            return false;
        }
        if (needAwareness > 0 && (casino == null || casino.getAwareness() < needAwareness)) {
            log.warn("扣除道具失败, 知名度不足 playerId={},need={},have={}",
                    playerId, needAwareness, casino == null ? 0 : casino.getAwareness());
            return false;
        }
        if (needSeasonCoin > 0 && (seasonData == null || seasonData.getSeasonCoin() < needSeasonCoin)) {
            log.warn("扣除道具失败, 赛季币不足 playerId={},need={},have={}",
                    playerId, needSeasonCoin, seasonData == null ? 0 : seasonData.getSeasonCoin());
            return false;
        }
        //校验通过后统一扣除
        if (needPower > 0) {
            base.setPower(base.getPower() - (int) needPower);
        }
        if (needAwareness > 0) {
            casino.setAwareness(casino.getAwareness() - (int) needAwareness);
        }
        if (needSeasonCoin > 0) {
            seasonEconomyService.spend(seasonData, needSeasonCoin);
        }
        return true;
    }

    /**
     * 离线（玩家不在本节点）扣除：直写持久化数据。
     * <p>
     * 与离线入账同理，先取齐载体并校验充足，任一不满足就整体失败且不落任何一笔。
     * 赛季币只减余额、不动 totalEarnedCoin，与在线的 {@link SeasonEconomyService#spend} 一致。
     */
    private boolean removeItemsOffline(long playerId, long needPower, long needAwareness, long needSeasonCoin) {
        SimBaseData base = needPower > 0 || needAwareness > 0
                ? simPlayerGameDao.findById(playerId).orElse(null) : null;
        if ((needPower > 0 || needAwareness > 0) && base == null) {
            log.warn("离线扣除sim资源失败, 无SimBaseData playerId={}", playerId);
            return false;
        }
        if (needPower > 0 && base.getPower() < needPower) {
            log.warn("离线扣除失败, 能量不足 playerId={},need={},have={}", playerId, needPower, base.getPower());
            return false;
        }
        SimCasinoData casino = needAwareness > 0 ? findOfflineCasino(playerId, base.getCurrentCasinoId()) : null;
        if (needAwareness > 0 && (casino == null || casino.getAwareness() < needAwareness)) {
            log.warn("离线扣除失败, 知名度不足 playerId={},need={},have={}",
                    playerId, needAwareness, casino == null ? 0 : casino.getAwareness());
            return false;
        }
        SeasonPlayerData seasonData = needSeasonCoin > 0 ? seasonPlayerDao.findById(playerId).orElse(null) : null;
        if (needSeasonCoin > 0 && (seasonData == null || seasonData.getSeasonCoin() < needSeasonCoin)) {
            log.warn("离线扣除失败, 赛季币不足 playerId={},need={},have={}",
                    playerId, needSeasonCoin, seasonData == null ? 0 : seasonData.getSeasonCoin());
            return false;
        }

        if (needPower > 0) {
            base.setPower(base.getPower() - (int) needPower);
            simPlayerGameDao.save(base);
        }
        if (casino != null) {
            casino.setAwareness(casino.getAwareness() - (int) needAwareness);
            simCasinoDao.save(casino);
        }
        if (seasonData != null) {
            seasonData.setSeasonCoin(seasonData.getSeasonCoin() - needSeasonCoin);
            seasonPlayerDao.save(seasonData);
        }
        return true;
    }

    /**
     * 取 SimBaseData: 在线取内存态, 否则读库
     */
    private SimBaseData getBaseData(long playerId, SimPlayerContext ctx) {
        if (ctx != null && ctx.getSimBaseData() != null) {
            return ctx.getSimBaseData();
        }
        return simPlayerGameDao.findById(playerId).orElse(null);
    }

    /**
     * 取当前场景: 在线取内存态, 否则读库（currentCasinoId 失效则回退任一场景）
     */
    private SimCasinoData getCurrentCasino(long playerId, SimPlayerContext ctx) {
        if (ctx != null && ctx.getCurrentCasino() != null) {
            return ctx.getCurrentCasino();
        }
        SimBaseData base = simPlayerGameDao.findById(playerId).orElse(null);
        if (base == null) {
            return null;
        }
        SimCasinoData casino = base.getCurrentCasinoId() > 0
                ? simCasinoDao.findOne(playerId, base.getCurrentCasinoId()) : null;
        return casino == null ? simCasinoDao.findFirstByPlayerId(playerId) : casino;
    }
}
