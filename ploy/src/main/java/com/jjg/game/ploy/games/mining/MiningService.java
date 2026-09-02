package com.jjg.game.ploy.games.mining;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.numeric.*;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.ploy.games.mining.message.*;
import com.jjg.game.ploy.manager.StandalonePloyGame;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

/** 服务端权威挖矿。所有写操作使用玩家分布式锁和背包存档CAS。 */
@Service
public class MiningService implements OrderGenerate, StandalonePloyGame {
    private static final Logger log = LoggerFactory.getLogger(MiningService.class);
    private static final SecureRandom SEEDS = new SecureRandom();
    private final MiningConfig config;
    private final PlayerPackService packs;
    private final RedissonClient redis;
    private final MiningRankService ranks;
    private final MiningAdTicketService ads;

    public MiningService(MiningConfig config, PlayerPackService packs, RedissonClient redis,
                         MiningRankService ranks, MiningAdTicketService ads) {
        this.config = config; this.packs = packs; this.redis = redis; this.ranks = ranks; this.ads = ads;
    }

    private record Snapshot(String json, MiningState state) { }
    private record ProductView(boolean available, String disabledReason, int id, int order,
                               int boughtToday, int remaining, List<ItemInfo> goods) { }

    public ResMiningState info(Player player) { return respond(player, null); }
    public ResMiningState action(Player player, ReqMiningAction request) { return respond(player, request); }

    private ResMiningState respond(Player player, ReqMiningAction request) {
        ResMiningState response = new ResMiningState(Code.SUCCESS);
        response.action = request == null ? 0 : request.action;
        try {
            return inSeason(player, season -> {
                Snapshot snapshot = load(player, season);
                MiningState state = snapshot.state;
                MiningEngine engine = new MiningEngine();
                Map<Integer, Long> costs = Map.of(), rewards = Map.of();
                AddType source = AddType.MINING_DIG;
                if (request != null) {
                    if (!Objects.equals(request.seasonId, state.seasonId) || request.version != state.version)
                        throw new MiningException(Code.REPEAT_OP, "STALE_VERSION");
                    if (state.delivery != null) throw new MiningException(Code.FAIL, "DELIVERY_REQUIRES_RECONCILIATION");
                    if (request.count < 1 || request.count > 10000 || request.action != MiningConstant.EXCHANGE && request.count != 1)
                        throw new MiningException("INVALID_COUNT");
                    switch (request.action) {
                        case MiningConstant.DIG -> {
                            MiningEngine.DigResult result = engine.dig(state, request.row, request.column, request.id, System.currentTimeMillis());
                            costs = Map.of(result.itemId(), 1L); rewards = result.rewards();
                            MiningState hitState = state;
                            response.scrollRows = result.scrollRows();
                            response.rewardCells = result.rewardCells().stream().map(cellReward -> {
                                MiningRewardCellInfo info = new MiningRewardCellInfo();
                                info.row = cellReward.cell().row;
                                info.column = cellReward.cell().column;
                                info.typeId = cellReward.cell().type;
                                info.rewards = ItemUtils.buildItemInfo(cellReward.rewards());
                                return info;
                            }).toList();
                            int visibleBottom = Math.addExact(hitState.topRow, hitState.visibleRows);
                            response.changed = result.changed().stream()
                                    .filter(c -> c.row >= hitState.topRow && c.row < visibleBottom)
                                    .map(c -> cellInfo(c, hitState, engine)).toList();
                        }
                        case MiningConstant.EXCHANGE -> {
                            MiningExchangeShopCfg good = GameDataManager.getMiningExchangeShopCfg(request.id);
                            if (good == null) throw new MiningException("UNKNOWN_PRODUCT");
                            checkLimit(state, good.getId(), good.getDailyPurchaseLimit(), request.count);
                            costs = multiply(MiningCatalog.itemPair(good.getCost()), request.count);
                            rewards = multiply(MiningCatalog.itemPair(good.getGoods()), request.count);
                            if (costs.isEmpty() || rewards.isEmpty()) throw new MiningException(Code.SAMPLE_ERROR, "INVALID_SHOP_CONFIG");
                            purchase(state, good.getId(), request.count);
                            state.total.exchanges += request.count; state.daily.exchanges += request.count;
                            for (Map.Entry<Integer, Long> reward : rewards.entrySet()) {
                                state.total.exchangedItems.merge(reward.getKey(), reward.getValue(), Math::addExact);
                                state.daily.exchangedItems.merge(reward.getKey(), reward.getValue(), Math::addExact);
                            }
                            source = AddType.MINING_EXCHANGE;
                        }
                        case MiningConstant.BUNDLE -> {
                            MiningBundleShopCfg good = GameDataManager.getMiningBundleShopCfg(request.id);
                            if (good == null) throw new MiningException("UNKNOWN_PRODUCT");
                            checkLimit(state, good.getId(), good.getDailyPurchaseLimit(), 1);
                            int mode = bundleMode(good);
                            if (mode == 3)
                                throw new MiningException(Code.FORBID, "PAYMENT_REQUIRED");
                            if (price(good).signum() != 0) throw new MiningException(Code.SAMPLE_ERROR, "FREE_BUNDLE_HAS_PRICE");
                            if (mode == 2) {
                                if (!ads.valid(player.getId(), state.day, request.adTicket) || state.usedAdTickets.contains(request.adTicket))
                                    throw new MiningException(Code.FORBID, "INVALID_AD_TICKET");
                                state.usedAdTickets.add(request.adTicket);
                                state.total.ads++; state.daily.ads++;
                            }
                            purchase(state, good.getId(), 1);
                            rewards = MiningCatalog.itemPair(good.getGoods()); source = AddType.MINING_BUNDLE;
                        }
                        case MiningConstant.ACHIEVEMENT -> {
                            MiningAchievementCfg achievement = GameDataManager.getMiningAchievementCfg(request.id);
                            if (achievement == null) throw new MiningException("UNKNOWN_ACHIEVEMENT");
                            MiningTaskInfo task = achievementInfo(state, achievement);
                            if (task.status != 1) throw new MiningException(task.status == 2 ? Code.REPEAT_OP : Code.ERROR_REQ, "ACHIEVEMENT_NOT_CLAIMABLE");
                            state.claimedAchievements.add(request.id);
                            rewards = achievement.getReward(); source = AddType.MINING_ACHIEVEMENT;
                        }
                        case MiningConstant.DAILY_TASK -> {
                            MiningConfig.DailyTask task = config.dailyTasks.stream().filter(t -> t.id == request.id).findFirst()
                                    .orElseThrow(() -> new MiningException("UNKNOWN_TASK"));
                            if (state.claimedDailyTasks.contains(task.id)) throw new MiningException(Code.REPEAT_OP, "TASK_ALREADY_CLAIMED");
                            if (dailyProgress(state.daily, task) < task.target) throw new MiningException("TASK_NOT_COMPLETE");
                            state.claimedDailyTasks.add(task.id); rewards = task.rewards; source = AddType.MINING_DAILY_TASK;
                        }
                        default -> throw new MiningException("UNKNOWN_ACTION");
                    }
                    validateItems(costs); validateItems(rewards);
                    if (rewards.keySet().stream().anyMatch(id -> !ordinaryItem(id))) {
                        // 兑换支持能量、游客、雇员等特殊道具；这些由现有业务处理器入账。
                        snapshot = deliverSpecial(player, snapshot, costs, rewards, request.id, source);
                        state = snapshot.state;
                    } else snapshot = commit(player, snapshot.json, state, costs, rewards, source);
                    response.rewards = ItemUtils.buildItemInfo(rewards);
                    log.info("mining_action playerId={} action={} id={} depth={} version={} rewards={}",
                            player.getId(), request.action, request.id, state.depth, state.version, rewards);
                }
                // 投影失败不反转已提交的背包操作；下次请求/赛季结算会从存档恢复。
                try { ranks.sync(player.getId(), state); }
                catch (Exception e) { log.error("挖矿排行投影失败 playerId={}", player.getId(), e); }
                response.info = buildInfo(player, state, season, engine);
                return response;
            });
        } catch (MiningException e) {
            response.code = e.code; response.reason = e.getMessage(); response.changed = null; response.rewardCells = null;
        } catch (Exception e) {
            response.code = Code.EXCEPTION; response.reason = "SERVER_ERROR_REFRESH_STATE";
            log.error("挖矿请求异常 playerId={}", player.getId(), e);
        }
        return response;
    }

    public ResMiningRank rank(Player player) {
        try { return inSeason(player, season -> {
            Snapshot snapshot = load(player, season);
            ranks.sync(player.getId(), snapshot.state);
            ResMiningRank response = ranks.rank(player, season);
            response.version = snapshot.state.version;
            return response;
        }); } catch (MiningException e) { return new ResMiningRank(e.code); }
        catch (Exception e) { log.error("挖矿排行查询失败 playerId={}", player.getId(), e); return new ResMiningRank(Code.EXCEPTION); }
    }

    public ResMiningExchangeShop exchangeShop(Player player) {
        try { return inSeason(player, season -> {
            Snapshot snapshot = load(player, season);
            MiningState state = snapshot.state;
            ResMiningExchangeShop response = new ResMiningExchangeShop(Code.SUCCESS);
            response.seasonId = state.seasonId; response.version = state.version;
            response.nextDailyReset = nextDailyReset();
            Set<Integer> currencyIds = new TreeSet<>();
            response.goods = GameDataManager.getMiningExchangeShopCfgList().stream().map(good -> {
                currencyIds.addAll(MiningCatalog.itemPair(good.getCost()).keySet());
                return exchangeInfo(state, good);
            }).sorted(Comparator.comparingInt((MiningExchangeInfo good) -> good.order).thenComparingInt(good -> good.id)).toList();
            response.currencies = ItemUtils.buildItemInfo(balances(player, currencyIds));
            return response;
        }); } catch (MiningException e) { return new ResMiningExchangeShop(e.code); }
        catch (Exception e) { log.error("挖矿兑换商店查询失败 playerId={}", player.getId(), e); return new ResMiningExchangeShop(Code.EXCEPTION); }
    }

    public ResMiningBundleShop bundleShop(Player player) {
        try { return inSeason(player, season -> {
            Snapshot snapshot = load(player, season);
            MiningState state = snapshot.state;
            ResMiningBundleShop response = new ResMiningBundleShop(Code.SUCCESS);
            response.seasonId = state.seasonId; response.version = state.version;
            response.nextDailyReset = nextDailyReset();
            response.bundles = GameDataManager.getMiningBundleShopCfgList().stream().map(good -> bundleInfo(state, good))
                    .sorted(Comparator.comparingInt((MiningBundleInfo good) -> good.order).thenComparingInt(good -> good.id)).toList();
            return response;
        }); } catch (MiningException e) { return new ResMiningBundleShop(e.code); }
        catch (Exception e) { log.error("挖矿礼包查询失败 playerId={}", player.getId(), e); return new ResMiningBundleShop(Code.EXCEPTION); }
    }

    public ResMiningAchievements achievements(Player player) {
        try { return inSeason(player, season -> {
            MiningState state = load(player, season).state;
            ResMiningAchievements response = new ResMiningAchievements(Code.SUCCESS);
            response.seasonId = state.seasonId; response.version = state.version;
            response.achievements = GameDataManager.getMiningAchievementCfgList().stream().map(c -> achievementInfo(state, c))
                    .sorted(taskOrder()).toList();
            return response;
        }); } catch (MiningException e) { return new ResMiningAchievements(e.code); }
        catch (Exception e) { log.error("挖矿成就查询失败 playerId={}", player.getId(), e); return new ResMiningAchievements(Code.EXCEPTION); }
    }

    public ResMiningDailyTasks dailyTasks(Player player) {
        try { return inSeason(player, season -> {
            MiningState state = load(player, season).state;
            ResMiningDailyTasks response = new ResMiningDailyTasks(Code.SUCCESS);
            response.seasonId = state.seasonId; response.version = state.version;
            response.nextDailyReset = nextDailyReset(); response.dailyTasks = dailyTaskInfos(state);
            return response;
        }); } catch (MiningException e) { return new ResMiningDailyTasks(e.code); }
        catch (Exception e) { log.error("挖矿每日任务查询失败 playerId={}", player.getId(), e); return new ResMiningDailyTasks(Code.EXCEPTION); }
    }

    private <T> T inSeason(Player player, Function<MiningConfig.Season, T> operation) {
        if (!config.enabled) throw new MiningException(Code.FORBID, "MINING_CLOSED");
        MiningConfig.Season season = config.currentSeason(System.currentTimeMillis());
        if (season == null) throw new MiningException(Code.EXPIRE, "NO_ACTIVE_SEASON");
        RLock seasonLock = ranks.seasonReadLock(season.id);
        if (!seasonLock.tryLock()) throw new MiningException(Code.FAIL, "SEASON_SETTLING");
        RLock lock = redis.getLock("mining:player:" + player.getId());
        boolean acquired = false;
        try {
            acquired = lock.tryLock();
            if (!acquired) throw new MiningException(Code.FAIL, "PLAYER_BUSY");
            MiningConfig.Season current = config.currentSeason(System.currentTimeMillis());
            if (current == null || !current.id.equals(season.id)) throw new MiningException(Code.EXPIRE, "SEASON_ENDED");
            return operation.apply(season);
        } finally { if (acquired) lock.unlock(); seasonLock.unlock(); }
    }

    private Snapshot load(Player player, MiningConfig.Season season) {
        PlayerPack pack = packs.getFromAllDB(player.getId());
        if (pack == null) throw new MiningException(Code.NOT_FOUND, "PLAYER_PACK_MISSING");
        String json = pack.getMiningState();
        MiningState state = json == null ? null : JSON.parseObject(json, MiningState.class);
        boolean changed = state == null;
        if (state == null || !season.id.equals(state.seasonId)) {
            MiningState previous = state;
            if (previous != null) {
                if (previous.delivery != null) throw new MiningException(Code.FAIL, "DELIVERY_REQUIRES_RECONCILIATION");
                // 归档前必须保存上一赛季最终深度，否则新存档会覆盖结算恢复源。
                ranks.sync(player.getId(), previous);
            }
            state = new MiningEngine().create(season.id, SEEDS.nextLong());
            if (previous != null) {
                state.version = previous.version;
                state.total = previous.total; state.claimedAchievements = previous.claimedAchievements;
                state.permanentPurchases = previous.permanentPurchases;
                state.paymentQuotes = previous.paymentQuotes; state.paidOrders = previous.paidOrders;
                state.day = previous.day; state.daily = previous.daily;
                state.dailyPurchases = previous.dailyPurchases; state.claimedDailyTasks = previous.claimedDailyTasks;
                state.usedAdTickets = previous.usedAdTickets;
            }
            ranks.register(player, season.id);
            changed = true;
        }
        if (state.delivery == null && state.day != TimeHelper.getDayNumerical()) {
            state.refreshDay(TimeHelper.getDayNumerical()); changed = true;
        }
        return changed ? commit(player, json, state, Map.of(), Map.of(), AddType.MINING_DIG) : new Snapshot(json, state);
    }

    private Snapshot commit(Player player, String expected, MiningState state, Map<Integer, Long> costs,
                            Map<Integer, Long> rewards, AddType source) {
        state.version = Math.addExact(state.version, 1);
        String next = JSON.toJSONString(state);
        CommonResult<ItemOperationResult> result = packs.exchangeMiningItems(player, costs, rewards, source,
                "mining:" + state.seasonId + ":" + state.version, expected, next);
        if (!result.success()) throw new MiningException(result.code, result.code == Code.REPEAT_OP ? "STALE_VERSION" : "ITEM_TRANSACTION_FAILED");
        return new Snapshot(next, state);
    }

    private Snapshot deliverSpecial(Player player, Snapshot before, Map<Integer, Long> costs,
                                    Map<Integer, Long> rewards, int goodId, AddType source) {
        MiningState.Delivery delivery = new MiningState.Delivery();
        delivery.id = UUID.randomUUID().toString(); delivery.costs = costs; delivery.rewards = rewards; delivery.goodId = goodId;
        delivery.previousState = before.json;
        before.state.delivery = delivery;
        Snapshot pending = commit(player, before.json, before.state, costs, Map.of(), source);
        // 未知结果必须保留pending，不能猜测失败并自动退款/重发。
        CommonResult<ItemOperationResult> grant = packs.addItems(player.getId(), rewards, source, "mining:delivery:" + delivery.id);
        if (!grant.success()) {
            MiningState restored = JSON.parseObject(before.json, MiningState.class);
            restored.version = pending.state.version;
            commit(player, pending.json, restored, Map.of(), costs, AddType.FAIL_ROLLBACK);
            throw new MiningException(grant.code, "SPECIAL_REWARD_FAILED_REFUNDED");
        }
        pending.state.delivery = null;
        return commit(player, pending.json, pending.state, Map.of(), Map.of(), source);
    }

    /** 运维核账确认后调用；不是客户端协议。granted=true表示外部奖励已到账。 */
    public void reconcileDelivery(Player player, String deliveryId, boolean granted) {
        RLock lock = redis.getLock("mining:player:" + player.getId());
        if (!lock.tryLock()) throw new MiningException("PLAYER_BUSY");
        try {
            PlayerPack pack = packs.getFromAllDB(player.getId());
            MiningState state = JSON.parseObject(pack.getMiningState(), MiningState.class);
            if (state.delivery == null || !Objects.equals(state.delivery.id, deliveryId)) throw new MiningException("DELIVERY_NOT_FOUND");
            MiningState.Delivery delivery = state.delivery;
            if (!granted) {
                MiningState restored = JSON.parseObject(delivery.previousState, MiningState.class);
                restored.version = state.version;
                state = restored;
            }
            state.delivery = null;
            commit(player, pack.getMiningState(), state, Map.of(), granted ? Map.of() : delivery.costs, AddType.FAIL_ROLLBACK);
        } finally { lock.unlock(); }
    }

    @Override public RechargeType getRechargeType() { return RechargeType.MINING_BUNDLE; }

    @Override public int gameType() { return MiningConstant.GAME_ID; }

    @Override
    public void onOrderCreationFailed(Player player, ReqGenerateOrder request) {
        releasePaymentQuote(player, request.desc);
    }

    /** 仅由服务端在订单状态已确认为取消/失败后调用，不接受客户端自行取消配额。 */
    public void releaseClosedOrder(Player player, Order order) {
        if (order.getPlayerId() != player.getId() || order.getRechargeType() != RechargeType.MINING_BUNDLE
                || (order.getOrderStatus() != OrderStatus.CANCEL && order.getOrderStatus() != OrderStatus.FAIL))
            throw new MiningException("ORDER_NOT_CLOSED");
        releasePaymentQuote(player, order.getDesc());
    }

    private void releasePaymentQuote(Player player, String quoteId) {
        RLock lock = redis.getLock("mining:player:" + player.getId());
        if (!lock.tryLock()) throw new MiningException("PLAYER_BUSY");
        try {
            PlayerPack pack = packs.getFromAllDB(player.getId());
            if (pack == null || pack.getMiningState() == null) return;
            MiningState state = JSON.parseObject(pack.getMiningState(), MiningState.class);
            if (state.delivery != null) throw new MiningException("DELIVERY_REQUIRES_RECONCILIATION");
            MiningState.Quote quote = state.paymentQuotes.remove(quoteId);
            if (quote == null) return;
            if (state.day == quote.day) state.dailyPurchases.merge(quote.goodId, -1, Integer::sum);
            state.permanentPurchases.merge(quote.goodId, -1, Integer::sum);
            commit(player, pack.getMiningState(), state, Map.of(), Map.of(), AddType.MINING_BUNDLE);
        } finally { lock.unlock(); }
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder request) {
        try { return inSeason(player, season -> {
            MiningBundleShopCfg good = GameDataManager.getMiningBundleShopCfg(Integer.parseInt(request.productId));
            if (good == null || bundleMode(good) != 3) return null;
            BigDecimal price = price(good);
            if (price.signum() <= 0) return null;
            Map<Integer, Long> goods = MiningCatalog.itemPair(good.getGoods());
            validateItems(goods);
            if (goods.isEmpty() || goods.keySet().stream().anyMatch(id -> !ordinaryItem(id))) return null;
            Snapshot snapshot = load(player, season);
            if (snapshot.state.delivery != null) return null;
            checkLimit(snapshot.state, good.getId(), good.getDailyPurchaseLimit(), 1);
            MiningState.Quote quote = new MiningState.Quote();
            quote.id = UUID.randomUUID().toString(); quote.goodId = good.getId(); quote.day = snapshot.state.day;
            quote.price = price.toPlainString(); quote.goods = goods;
            snapshot.state.paymentQuotes.put(quote.id, quote);
            // 下单即占用限购额度，避免并发创建多张订单绕过限购；废单需按订单状态释放。
            purchase(snapshot.state, good.getId(), 1);
            commit(player, snapshot.json, snapshot.state, Map.of(), Map.of(), AddType.MINING_BUNDLE);
            request.desc = quote.id;
            return price;
        }); } catch (Exception e) { log.warn("挖矿礼包预下单拒绝 playerId={} productId={}", player.getId(), request.productId, e); return null; }
    }

    @Override
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getPlayerId() != player.getId() || order.getRechargeType() != RechargeType.MINING_BUNDLE
                || order.getId() == null || order.getPrice() == null) return false;
        // 已支付的旧赛季订单照常履约，不受玩法关闭影响。
        RLock lock = redis.getLock("mining:player:" + player.getId());
        if (!lock.tryLock()) return false;
        try {
            PlayerPack pack = packs.getFromAllDB(player.getId());
            if (pack == null || pack.getMiningState() == null) return false;
            MiningState state = JSON.parseObject(pack.getMiningState(), MiningState.class);
            if (state.paidOrders.containsKey(order.getId())) return true;
            if (state.delivery != null) return false;
            MiningState.Quote quote = state.paymentQuotes.get(order.getDesc());
            if (quote == null || !Integer.toString(quote.goodId).equals(order.getProductId())
                    || new BigDecimal(quote.price).compareTo(order.getPrice()) != 0) return false;
            state.paymentQuotes.remove(quote.id);
            state.paidOrders.put(order.getId(), System.currentTimeMillis());
            commit(player, pack.getMiningState(), state, Map.of(), quote.goods, AddType.MINING_BUNDLE);
            return true;
        } catch (Exception e) { log.error("挖矿礼包支付发货失败 orderId={}", order.getId(), e); return false; }
        finally { lock.unlock(); }
    }

    private MiningInfo buildInfo(Player player, MiningState state, MiningConfig.Season season, MiningEngine engine) {
        MiningInfo info = new MiningInfo();
        info.seasonId = state.seasonId; info.seasonEndTime = season.endTime; info.version = state.version;
        info.width = state.width; info.visibleRows = state.visibleRows; info.topRow = state.topRow; info.depth = state.depth;
        info.nextDailyReset = nextDailyReset();
        info.cells = state.cells.stream().filter(c -> c.row < state.topRow + state.visibleRows)
                .map(c -> cellInfo(c, state, engine)).toList();
        Map<Integer, Long> tools = balances(player, engine.toolItemIds());
        info.tools = ItemUtils.buildItemInfo(tools);
        info.ores = ItemUtils.buildItemInfo(balances(player, engine.resourceItemIds()));
        info.toolsExhausted = tools.values().stream().allMatch(count -> count == 0);
        info.totalGrids = state.total.grids;
        info.resourceValue = engine.resourceItemIds().stream()
                .mapToLong(id -> state.total.resources.getOrDefault(id, 0L)).sum();
        info.pendingDeliveryId = state.delivery == null ? null : state.delivery.id;
        return info;
    }

    private Map<Integer, Long> balances(Player player, Collection<Integer> itemIds) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        itemIds.stream().sorted().forEach(id -> result.put(id, packs.getItemCount(player.getId(), id)));
        return result;
    }

    private static long nextDailyReset() {
        return LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private MiningExchangeInfo exchangeInfo(MiningState state, MiningExchangeShopCfg good) {
        ProductView product = productView(state, good.getId(), good.getDailyPurchaseLimit(), good.getGoods(), good.getOrder());
        MiningExchangeInfo info = new MiningExchangeInfo();
        info.available = product.available; info.disabledReason = product.disabledReason;
        info.id = product.id; info.order = product.order; info.boughtToday = product.boughtToday;
        info.remaining = product.remaining; info.goods = product.goods;
        info.cost = ItemUtils.buildItemInfo(MiningCatalog.itemPair(good.getCost()));
        return info;
    }

    private MiningBundleInfo bundleInfo(MiningState state, MiningBundleShopCfg good) {
        ProductView product = productView(state, good.getId(), good.getDailyPurchaseLimit(), good.getGoods(), good.getOrder());
        MiningBundleInfo info = new MiningBundleInfo();
        info.available = product.available; info.disabledReason = product.disabledReason;
        info.id = product.id; info.order = product.order; info.boughtToday = product.boughtToday;
        info.remaining = product.remaining; info.goods = product.goods;
        info.mode = bundleMode(good); info.price = price(good).toPlainString();
        return info;
    }

    private ProductView productView(MiningState state, int id, int limit, List<Integer> goods, int order) {
        Map<Integer, Long> items = MiningCatalog.itemPair(goods);
        boolean available = true; String disabledReason = null;
        try { validateItems(items); available = !items.isEmpty(); }
        catch (MiningException e) { available = false; disabledReason = e.getMessage(); }
        return new ProductView(available, disabledReason, id, order, state.dailyPurchases.getOrDefault(id, 0),
                remaining(state, id, limit), ItemUtils.buildItemInfo(items));
    }

    private List<MiningTaskInfo> dailyTaskInfos(MiningState state) {
        return config.dailyTasks.stream().map(task -> {
            MiningTaskInfo info = new MiningTaskInfo(); info.id = task.id; info.target = task.target;
            info.kind = task.kind; info.itemId = task.itemId;
            info.nameLanguageId = task.nameLanguageId; info.descLanguageId = task.descLanguageId;
            info.progress = Math.min(task.target, dailyProgress(state.daily, task));
            info.status = state.claimedDailyTasks.contains(task.id) ? 2 : info.progress >= info.target ? 1 : 0;
            info.rewards = ItemUtils.buildItemInfo(task.rewards); return info;
        }).sorted(taskOrder()).toList();
    }

    static MiningCellInfo cellInfo(MiningState.Cell c, MiningState state, MiningEngine engine) {
        MiningCellInfo info = new MiningCellInfo(); info.row = c.row; info.column = c.column; info.typeId = c.type;
        info.hp = c.hp; info.secretId = c.secretId;
        info.connected = c.hp > 0 && !engine.isWallCell(c.type) && engine.connected(state, c.row, c.column);
        return info;
    }

    static MiningTaskInfo achievementInfo(MiningState state, MiningAchievementCfg cfg) {
        ConditionSpec configured = ConditionSpec.from(cfg.getAllianceTaskConditionId());
        ConditionSpec normalized = normalizeAchievementCondition(configured);
        PreparedCondition condition = ConditionRuleRegistry.standard().prepare(normalized);
        ConditionSpec spec = condition.spec(); MiningState.Stats stats = state.total;
        int related = spec.parameters().size() > 2 ? spec.intParameter(1) : 0;
        ActionConditionEvent.Type type; long value;
        switch (spec.id()) {
            case 12701 -> { type = ActionConditionEvent.Type.GRID_MINED; value = stats.grids; }
            case 12702 -> { type = ActionConditionEvent.Type.DEPTH_REACHED; value = stats.depth; }
            case 12703 -> { type = ActionConditionEvent.Type.ITEM_USE; value = related == 0 ? stats.tools.values().stream().mapToLong(Long::longValue).sum() : stats.tools.getOrDefault(related, 0L); }
            case 12704 -> { type = ActionConditionEvent.Type.ITEM_EXCHANGE; value = related == 0 ? stats.exchanges : stats.exchangedItems.getOrDefault(related, 0L); }
            default -> throw new MiningException(Code.SAMPLE_ERROR, "UNSUPPORTED_ACHIEVEMENT");
        }
        int subject = spec.intParameter(0);
        long progress = condition.evaluate(new ActionConditionEvent(type, subject, related, value, value, 0, false)).apply(0);
        MiningTaskInfo info = new MiningTaskInfo(); info.id = cfg.getId(); info.target = condition.target();
        info.nameLanguageId = cfg.getAchievementName(); info.descLanguageId = cfg.getAchievementDescription();
        info.progress = Math.min(progress, info.target); info.rewards = ItemUtils.buildItemInfo(cfg.getReward());
        info.status = state.claimedAchievements.contains(cfg.getId()) ? 2 : progress >= info.target ? 1 : 0;
        return info;
    }

    /** MiningAchievement 表省略通用 subject 参数时，按“不限制主体”补 0。 */
    private static ConditionSpec normalizeAchievementCondition(ConditionSpec spec) {
        int expected = switch (spec.id()) {
            case 12701, 12702 -> 2;
            case 12703, 12704 -> 3;
            default -> 0;
        };
        if (expected == 0 || spec.parameters().size() != expected - 1) return spec;
        List<Long> parameters = new ArrayList<>(expected);
        parameters.add(0L);
        parameters.addAll(spec.parameters());
        return new ConditionSpec(spec.id(), parameters);
    }

    static long dailyProgress(MiningState.Stats stats, MiningConfig.DailyTask task) {
        if (task.target <= 0) throw new MiningException(Code.SAMPLE_ERROR, "INVALID_DAILY_TASK");
        return switch (task.kind) {
            case 1 -> stats.grids;
            case 2 -> stats.depth;
            case 3 -> task.itemId == 0 ? stats.tools.values().stream().mapToLong(Long::longValue).sum() : stats.tools.getOrDefault(task.itemId, 0L);
            case 4 -> task.itemId == 0 ? stats.resources.values().stream().mapToLong(Long::longValue).sum() : stats.resources.getOrDefault(task.itemId, 0L);
            case 5 -> stats.ads;
            default -> throw new MiningException(Code.SAMPLE_ERROR, "INVALID_DAILY_TASK");
        };
    }

    private static Comparator<MiningTaskInfo> taskOrder() {
        return Comparator.comparingInt((MiningTaskInfo t) -> t.status == 1 ? 0 : t.status == 0 ? 1 : 2).thenComparingInt(t -> t.id);
    }
    private int remaining(MiningState state, int id, int dailyLimit) {
        int daily = dailyLimit > 0 ? Math.max(0, dailyLimit - state.dailyPurchases.getOrDefault(id, 0)) : Integer.MAX_VALUE;
        int permanentLimit = config.permanentLimits.getOrDefault(id, 0);
        int permanent = permanentLimit > 0 ? Math.max(0, permanentLimit - state.permanentPurchases.getOrDefault(id, 0)) : Integer.MAX_VALUE;
        int result = Math.min(daily, permanent); return result == Integer.MAX_VALUE ? -1 : result;
    }
    private void checkLimit(MiningState state, int id, int dailyLimit, int count) {
        int remaining = remaining(state, id, dailyLimit);
        if (remaining >= 0 && count > remaining) throw new MiningException(Code.DAILY_LIMIT, "PURCHASE_LIMIT");
    }
    private static void purchase(MiningState state, int id, int count) {
        state.dailyPurchases.merge(id, count, Math::addExact); state.permanentPurchases.merge(id, count, Math::addExact);
    }
    static Map<Integer, Long> multiply(Map<Integer, Long> values, int count) {
        if (count <= 0) throw new MiningException("INVALID_COUNT");
        Map<Integer, Long> result = new HashMap<>(); values.forEach((id, value) -> result.put(id, Math.multiplyExact(value, count))); return result;
    }
    /** 零元礼包按表内序列区分：第一项为免费领取，其余为广告领取；有价格的由支付回调发货。 */
    private static int bundleMode(MiningBundleShopCfg good) {
        if (price(good).signum() > 0) return 3;
        int freeId = GameDataManager.getMiningBundleShopCfgList().stream()
                .filter(cfg -> price(cfg).signum() == 0)
                .min(Comparator.comparingInt(MiningBundleShopCfg::getOrder).thenComparingInt(MiningBundleShopCfg::getId))
                .orElseThrow(() -> new MiningException(Code.SAMPLE_ERROR, "MISSING_FREE_BUNDLE"))
                .getId();
        return good.getId() == freeId ? 1 : 2;
    }
    private static BigDecimal price(MiningBundleShopCfg good) {
        List<Integer> cost = good.getCost();
        if (cost == null || cost.isEmpty()) return BigDecimal.ZERO;
        if (cost.size() != 1 || cost.getFirst() < 0) throw new MiningException(Code.SAMPLE_ERROR, "INVALID_BUNDLE_PRICE");
        return BigDecimal.valueOf(cost.getFirst());
    }
    private static boolean ordinaryItem(int id) {
        ItemCfg cfg = GameDataManager.getItemCfg(id);
        return cfg != null && cfg.getIsBag() && !GameConstant.suportSpecialItem(id) && !GameConstant.SIM_SPECIAL_ITEM_TYPE.contains(cfg.getItemType())
                && cfg.getType() != GameConstant.Item.TYPE_GOLD && cfg.getType() != GameConstant.Item.TYPE_DIAMOND
                && cfg.getType() != GameConstant.Item.TYPE_SHELL;
    }
    private static void validateItems(Map<Integer, Long> values) {
        if (values == null) throw new MiningException(Code.SAMPLE_ERROR, "NULL_REWARDS");
        values.forEach((id, count) -> {
            if (id == null || count == null || count <= 0 || GameDataManager.getItemCfg(id) == null)
                throw new MiningException(Code.SAMPLE_ERROR, "INVALID_REWARD_ITEM");
            ItemCfg cfg = GameDataManager.getItemCfg(id);
            if (!cfg.getIsBag() && !GameConstant.suportSpecialItem(id)
                    && !GameConstant.SIM_SPECIAL_ITEM_TYPE.contains(cfg.getItemType())
                    && cfg.getType() != GameConstant.Item.TYPE_GOLD && cfg.getType() != GameConstant.Item.TYPE_DIAMOND
                    && cfg.getType() != GameConstant.Item.TYPE_SHELL)
                throw new MiningException(Code.SAMPLE_ERROR, "UNSUPPORTED_NON_BAG_REWARD_" + id);
        });
    }
}
