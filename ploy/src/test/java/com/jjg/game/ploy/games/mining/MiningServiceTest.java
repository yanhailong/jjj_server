package com.jjg.game.ploy.games.mining;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.ploy.games.mining.message.*;
import com.jjg.game.sampledata.GameDataManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MiningServiceTest {
    private MiningService service;
    private PlayerPackService packs;
    private MiningRankService ranks;
    private MiningConfig config;
    private final Player player = new Player();
    private String saved;
    private Map<Integer, Long> wallet;

    @BeforeAll static void loadTables() throws Exception { MiningFixtures.install(); }
    @BeforeEach void setup() {
        player.setId(10001L); saved = null; wallet = new HashMap<>();
        wallet.put(1024034, 100L); wallet.put(1024035, 20L); wallet.put(1024036, 20L); wallet.put(1024037, 1000L);
        config = new MiningConfig(); packs = mock(PlayerPackService.class);
        ranks = mock(MiningRankService.class); RedissonClient redis = mock(RedissonClient.class); RLock lock = mock(RLock.class);
        when(lock.tryLock()).thenReturn(true); when(redis.getLock(anyString())).thenReturn(lock);
        when(ranks.seasonReadLock(anyString())).thenReturn(lock);
        when(packs.getFromAllDB(player.getId())).thenAnswer(inv -> { PlayerPack p = new PlayerPack(player.getId()); p.setMiningState(saved); return p; });
        when(packs.getItemCount(eq(player.getId()), anyInt())).thenAnswer(inv -> wallet.getOrDefault(inv.getArgument(1), 0L));
        when(packs.exchangeMiningItems(eq(player), anyMap(), anyMap(), any(), anyString(), nullable(String.class), anyString()))
                .thenAnswer(inv -> {
                    Map<Integer, Long> costs = inv.getArgument(1), rewards = inv.getArgument(2);
                    if (!Objects.equals(saved, inv.getArgument(5))) return new CommonResult<ItemOperationResult>(Code.REPEAT_OP);
                    if (costs.entrySet().stream().anyMatch(e -> wallet.getOrDefault(e.getKey(), 0L) < e.getValue()))
                        return new CommonResult<ItemOperationResult>(Code.NOT_ENOUGH_ITEM);
                    costs.forEach((id, n) -> wallet.merge(id, -n, Long::sum)); rewards.forEach((id, n) -> wallet.merge(id, n, Long::sum));
                    saved = inv.getArgument(6); return new CommonResult<ItemOperationResult>(Code.SUCCESS);
                });
        service = new MiningService(config, packs, redis, ranks);
        ResMiningState initial = service.info(player);
        var recoveryCfg = GameDataManager.getGlobalConfigCfg(MiningConstant.PICK_RECOVERY_INTERVAL_GLOBAL_ID);
        assertEquals(Code.SUCCESS, initial.code, initial.reason + ", recoveryCfg=" + recoveryCfg.getValue());
    }

    private MiningState state() { return JSON.parseObject(saved, MiningState.class); }
    private ReqMiningAction request(int action, int id) {
        ReqMiningAction request = new ReqMiningAction(); request.action = action; request.id = id;
        request.seasonId = state().seasonId; request.version = state().version; request.count = 1; request.row = 1; request.column = 1;
        return request;
    }

    @Test void mainStateIsLeanAndPanelsAreLoadedOnDemand() {
        ResMiningState main = service.info(player);
        Set<Integer> toolIds = new MiningEngine().toolItemIds();
        Set<Integer> oreIds = new MiningEngine().resourceItemIds();
        assertEquals(toolIds, main.info.tools.stream().map(item -> item.itemId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(oreIds, main.info.ores.stream().map(item -> item.itemId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(Collections.disjoint(toolIds, oreIds));

        ResMiningExchangeShop exchange = service.exchangeShop(player);
        assertEquals(Code.SUCCESS, exchange.code); assertFalse(exchange.goods.isEmpty()); assertFalse(exchange.currencies.isEmpty());
        assertEquals(state().version, exchange.version);

        ResMiningBundleShop bundles = service.bundleShop(player);
        assertEquals(Code.SUCCESS, bundles.code); assertFalse(bundles.bundles.isEmpty()); assertEquals(state().version, bundles.version);
        assertTrue(bundles.bundles.stream().allMatch(bundle -> bundle.nameLanguageId > 0));
        assertTrue(bundles.bundles.stream().allMatch(bundle -> bundle.adCdEndTime == 0));
        assertEquals(1, bundles.bundles.stream().filter(bundle -> bundle.id == 6001).findFirst().orElseThrow().mode);
        assertEquals(2, bundles.bundles.stream().filter(bundle -> bundle.id == 6002).findFirst().orElseThrow().mode);
        assertEquals(3, bundles.bundles.stream().filter(bundle -> bundle.id == 6003).findFirst().orElseThrow().mode);

        ResMiningAchievements achievements = service.achievements(player);
        assertEquals(Code.SUCCESS, achievements.code); assertFalse(achievements.achievements.isEmpty());
        assertTrue(achievements.achievements.stream().allMatch(info -> info.nameLanguageId > 0 && info.descLanguageId > 0));

        MiningConfig.DailyTask task = new MiningConfig.DailyTask(); task.id = 1; task.kind = 1;
        task.nameLanguageId = 400800061; task.descLanguageId = 400800062;
        task.target = 1; task.rewards = Map.of(1024034, 1L); config.dailyTasks = List.of(task);
        ResMiningDailyTasks dailyTasks = service.dailyTasks(player);
        assertEquals(Code.SUCCESS, dailyTasks.code); assertEquals(1, dailyTasks.dailyTasks.size());
        assertEquals(task.nameLanguageId, dailyTasks.dailyTasks.getFirst().nameLanguageId);
        assertEquals(task.descLanguageId, dailyTasks.dailyTasks.getFirst().descLanguageId);
    }

    @Test void bundledDailyTasksAreReturnedWhenExternalConfigIsMissing() {
        config.loadBundledDefaults();
        ResMiningDailyTasks response = service.dailyTasks(player);
        assertEquals(Code.SUCCESS, response.code);
        assertEquals(Set.of(1, 2, 3), response.dailyTasks.stream()
                .map(task -> task.id).collect(java.util.stream.Collectors.toSet()));
        assertTrue(response.dailyTasks.stream()
                .allMatch(task -> task.nameLanguageId > 0 && task.descLanguageId > 0));
    }

    @Test void initialInfoStartsPickRecoveryForBalanceOf189() {
        int pickItemId = new MiningEngine().pickItemId();
        wallet.put(pickItemId, 189L);
        MiningState current = state();
        current.nextPickRecoveryTime = 0;
        saved = JSON.toJSONString(current);
        long before = System.currentTimeMillis();

        ResMiningState response = service.info(player);

        assertEquals(Code.SUCCESS, response.code);
        assertTrue(response.info.nextPickRecoveryTime >= before + TimeUnit.MINUTES.toMillis(10));
        assertTrue(response.info.nextPickRecoveryTime <= System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
        assertEquals(response.info.nextPickRecoveryTime, state().nextPickRecoveryTime);
    }

    @Test void timedPickRecoveryUsesGlobalIntervalAndSettlesOfflineTimeOnce() {
        String[] configured = GameDataManager.getGlobalConfigCfg(
                MiningConstant.PICK_RECOVERY_INTERVAL_GLOBAL_ID).getValue().split("[,_，]");
        int configuredMinutes = Integer.parseInt(configured[0]);
        assertEquals(10, configuredMinutes); assertEquals(400, Integer.parseInt(configured[1]));
        long interval = TimeUnit.MINUTES.toMillis(configuredMinutes);
        long now = System.currentTimeMillis();
        MiningState current = state();
        current.nextPickRecoveryTime = now - interval - 1000;
        long previousVersion = current.version;
        saved = JSON.toJSONString(current);

        ResMiningState response = service.info(player);
        assertEquals(Code.SUCCESS, response.code);
        assertEquals(102, wallet.get(new MiningEngine().pickItemId()));
        assertEquals(2, response.rewards.stream()
                .filter(item -> item.itemId == new MiningEngine().pickItemId())
                .mapToLong(item -> item.count).sum());
        assertEquals(previousVersion + 1, state().version);
        assertTrue(response.info.nextPickRecoveryTime > now);
        assertTrue(response.info.nextPickRecoveryTime <= now + interval);

        String afterRecovery = saved;
        ResMiningState repeated = service.info(player);
        assertEquals(Code.SUCCESS, repeated.code);
        assertEquals(102, wallet.get(new MiningEngine().pickItemId()));
        assertEquals(afterRecovery, saved);
        assertTrue(repeated.rewards == null || repeated.rewards.isEmpty());
    }

    @Test void timedRecoveryStopsAtLimitAndRestartsAfterUsingAPick() {
        int pickItemId = new MiningEngine().pickItemId();
        wallet.put(pickItemId, 399L);
        MiningState current = state();
        current.nextPickRecoveryTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        saved = JSON.toJSONString(current);

        ResMiningState capped = service.info(player);
        assertEquals(Code.SUCCESS, capped.code);
        assertEquals(400, wallet.get(pickItemId));
        assertEquals(1, capped.rewards.stream().filter(item -> item.itemId == pickItemId)
                .mapToLong(item -> item.count).sum());
        assertEquals(0, capped.info.nextPickRecoveryTime);

        long beforeDig = System.currentTimeMillis();
        ResMiningState dug = service.action(player, request(MiningConstant.DIG, 101));
        assertEquals(Code.SUCCESS, dug.code);
        assertEquals(399, wallet.get(pickItemId));
        assertTrue(dug.info.nextPickRecoveryTime >= beforeDig + TimeUnit.MINUTES.toMillis(10));
    }

    @Test void duePickRecoveryCanFundTheSameVersionDigRequest() {
        int pickItemId = new MiningEngine().pickItemId();
        wallet.put(pickItemId, 0L);
        MiningState current = state();
        current.nextPickRecoveryTime = System.currentTimeMillis() - 1000;
        long previousVersion = current.version;
        saved = JSON.toJSONString(current);
        ReqMiningAction request = request(MiningConstant.DIG, 101);

        ResMiningState response = service.action(player, request);
        assertEquals(Code.SUCCESS, response.code);
        assertEquals(0, wallet.get(pickItemId));
        assertEquals(1, response.rewards.stream().filter(item -> item.itemId == pickItemId)
                .mapToLong(item -> item.count).sum());
        assertEquals(previousVersion + 2, response.info.version);
    }

    @Test void loadingOldTwoColumnStateMigratesItToConfiguredWidth() {
        MiningState old = state();
        old.width = 2;
        old.cells.removeIf(cell -> cell.column > 2);
        old.cells.getFirst().hp = 0;
        long version = old.version;
        saved = JSON.toJSONString(old);

        ResMiningState response = service.info(player);
        assertEquals(Code.SUCCESS, response.code);
        assertEquals(6, response.info.width);
        assertEquals(48, response.info.cells.size());
        assertEquals(0, response.info.cells.stream()
                .filter(cell -> cell.row == 1 && cell.column == 1).findFirst().orElseThrow().hp);
        assertEquals(version + 1, state().version);
    }

    @Test void duplicateDigAndStaleMapDoNotConsumeAgain() {
        ReqMiningAction request = request(MiningConstant.DIG, 101);
        ResMiningState first = service.action(player, request); assertEquals(Code.SUCCESS, first.code);
        Map<Integer, Long> after = Map.copyOf(wallet); long version = state().version;
        ResMiningState second = service.action(player, request);
        assertEquals("STALE_VERSION", second.reason); assertEquals(version, state().version); assertEquals(after, wallet);
    }

    @Test void digReturnsEachRewardWithItsSourceCell() {
        MiningState current = state();
        MiningState.Cell rewardCell = current.cells.stream()
                .filter(cell -> cell.row == 1 && cell.column == 1).findFirst().orElseThrow();
        rewardCell.type = 1004; rewardCell.hp = 1; saved = JSON.toJSONString(current);
        ResMiningState response = service.action(player, request(MiningConstant.DIG, 101));
        assertEquals(Code.SUCCESS, response.code);
        assertEquals(1, response.rewardCells.size());
        MiningRewardCellInfo source = response.rewardCells.getFirst();
        assertEquals(1, source.row); assertEquals(1, source.column); assertEquals(1004, source.typeId);
        assertEquals(1024037, source.rewards.getFirst().itemId); assertEquals(1, source.rewards.getFirst().count);
    }

    @Test void scrollReturnsOffsetAndOnlyVisibleChangedCells() {
        MiningState before = state();
        for (int row = before.topRow; row < before.topRow + before.visibleRows - 1; row++) {
            MiningState.Cell shaft = MiningFixtures.cell(before, row, 1);
            shaft.hp = 0;
            shaft.reachable = true;
        }
        saved = JSON.toJSONString(before);
        ReqMiningAction request = request(MiningConstant.DIG, 103);
        request.row = before.topRow + before.visibleRows - 1;
        ResMiningState response = service.action(player, request);
        assertEquals(Code.SUCCESS, response.code); assertEquals(1, response.scrollRows);
        assertEquals(before.topRow + response.scrollRows, response.info.topRow);
        assertTrue(response.changed.stream().allMatch(cell -> cell.row >= response.info.topRow
                && cell.row < response.info.topRow + response.info.visibleRows));
    }

    @Test void insufficientToolLeavesWholeStateUnchanged() {
        wallet.put(1024034, 0L); String before = saved;
        assertEquals(Code.NOT_ENOUGH_ITEM, service.action(player, request(MiningConstant.DIG, 101)).code);
        assertEquals(before, saved);
    }

    @Test void disconnectedTargetAndNegativeCountLeaveStateAndWalletUnchanged() {
        ReqMiningAction req = request(MiningConstant.DIG, 101); req.row = 8;
        String before = saved; Map<Integer, Long> balance = Map.copyOf(wallet);
        assertEquals("CELL_NOT_CONNECTED", service.action(player, req).reason);
        req = request(MiningConstant.EXCHANGE, 5004); req.count = -1;
        assertEquals("INVALID_COUNT", service.action(player, req).reason);
        assertEquals(before, saved); assertEquals(balance, wallet);
    }

    @Test void freeBundleLimitResetsOnDayChangeButPermanentLimitDoesNot() {
        config.permanentLimits = Map.of(6001, 2);
        assertEquals(Code.SUCCESS, service.action(player, request(MiningConstant.BUNDLE, 6001)).code);
        assertEquals(Code.DAILY_LIMIT, service.action(player, request(MiningConstant.BUNDLE, 6001)).code);
        MiningState next = state(); next.day = 20000101; saved = JSON.toJSONString(next);
        service.info(player);
        assertEquals(Code.SUCCESS, service.action(player, request(MiningConstant.BUNDLE, 6001)).code);
        next = state(); next.day = 20000101; saved = JSON.toJSONString(next); service.info(player);
        assertEquals(Code.DAILY_LIMIT, service.action(player, request(MiningConstant.BUNDLE, 6001)).code);
    }

    @Test void exchangeLimitsApplyToQuantityAndPersistProgressAtomically() {
        ReqMiningAction req = request(MiningConstant.EXCHANGE, 5004); req.count = 3;
        assertEquals(Code.SUCCESS, service.action(player, req).code);
        assertEquals(940, wallet.get(1024037)); assertEquals(3, wallet.get(1024008));
        assertEquals(3, state().total.exchanges);
        req = request(MiningConstant.EXCHANGE, 5004); req.count = 3;
        assertEquals(Code.DAILY_LIMIT, service.action(player, req).code);
        assertEquals(940, wallet.get(1024037));
    }

    @Test void unsupportedEntityRewardIsRejectedBeforeDeduction() {
        Map<Integer, Long> before = Map.copyOf(wallet); String old = saved;
        var res = service.action(player, request(MiningConstant.EXCHANGE, 5007));
        assertEquals(Code.SAMPLE_ERROR, res.code); assertTrue(res.reason.startsWith("UNSUPPORTED_NON_BAG_REWARD"));
        assertEquals(before, wallet); assertEquals(old, saved);
    }

    @Test void adBundleClaimsDirectlyAndPaidBundleCannotUseFreeClaim() {
        assertEquals(Code.SUCCESS, service.action(player, request(MiningConstant.BUNDLE, 6002)).code);
        assertEquals(1, state().total.ads);
        assertEquals("PAYMENT_REQUIRED", service.action(player, request(MiningConstant.BUNDLE, 6003)).reason);
    }

    @Test void exhaustedAdBundleReturnsDailyResetAsCooldownEndTime() {
        for (int i = 1; i <= 3; i++) {
            assertEquals(Code.SUCCESS, service.action(player, request(MiningConstant.BUNDLE, 6002)).code);
        }

        ResMiningBundleShop response = service.bundleShop(player);
        MiningBundleInfo adBundle = response.bundles.stream().filter(bundle -> bundle.id == 6002).findFirst().orElseThrow();
        assertEquals(400800040, adBundle.nameLanguageId);
        assertEquals(0, adBundle.remaining);
        assertEquals(response.nextDailyReset, adBundle.adCdEndTime);
    }

    @Test void achievementCountsFullDestroyedCellsAndCanBeClaimedOnlyOnce() {
        assertEquals(Code.ERROR_REQ, service.action(player, request(MiningConstant.ACHIEVEMENT, 1)).code);
        MiningState state = state(); state.total.grids = 100; saved = JSON.toJSONString(state);
        assertEquals(Code.SUCCESS, service.action(player, request(MiningConstant.ACHIEVEMENT, 1)).code);
        long balance = wallet.get(1024034);
        assertEquals(Code.REPEAT_OP, service.action(player, request(MiningConstant.ACHIEVEMENT, 1)).code);
        assertEquals(balance, wallet.get(1024034));
        assertEquals(100, MiningService.achievementInfo(state(), GameDataManager.getMiningAchievementCfg(1)).progress);
    }

    @Test void dailyTaskClaimUsesTodaysProgressAndCannotBeRepeated() {
        MiningConfig.DailyTask task = new MiningConfig.DailyTask(); task.id = 1; task.kind = 1;
        task.target = 50; task.rewards = Map.of(1024034, 5L); config.dailyTasks = List.of(task);
        assertEquals("TASK_NOT_COMPLETE", service.action(player, request(MiningConstant.DAILY_TASK, 1)).reason);
        MiningState state = state(); state.daily.grids = 50; saved = JSON.toJSONString(state);
        assertEquals(Code.SUCCESS, service.action(player, request(MiningConstant.DAILY_TASK, 1)).code);
        assertEquals(105, wallet.get(1024034));
        assertEquals(Code.REPEAT_OP, service.action(player, request(MiningConstant.DAILY_TASK, 1)).code);
        state = state(); state.day = 20000101; saved = JSON.toJSONString(state); service.info(player);
        assertEquals("TASK_NOT_COMPLETE", service.action(player, request(MiningConstant.DAILY_TASK, 1)).reason);
        assertEquals(105, wallet.get(1024034));
    }

    @Test void paymentQuoteLocksPriceRewardsAndDuplicateCallbackCannotGrantAgain() {
        ReqGenerateOrder req = new ReqGenerateOrder(); req.productId = "6003"; req.desc = "client supplied";
        CommonResult<BigDecimal> orderResult = service.generateOrderDetailInfo(player, req);
        assertTrue(orderResult.success());
        assertEquals(new BigDecimal("6"), orderResult.data);
        assertNotEquals("client supplied", req.desc);
        Order order = new Order(); order.setId("paid-order-1"); order.setPlayerId(player.getId());
        order.setRechargeType(RechargeType.MINING_BUNDLE); order.setProductId("6003"); order.setDesc(req.desc);
        order.setPrice(new BigDecimal("1"));
        assertFalse(service.onReceivedRecharge(player, order));
        order.setPrice(new BigDecimal("6"));
        assertTrue(service.onReceivedRecharge(player, order)); assertEquals(110, wallet.get(1024034));
        assertTrue(service.onReceivedRecharge(player, order)); assertEquals(110, wallet.get(1024034));
        assertEquals(1, state().dailyPurchases.get(6003));
        assertTrue(state().paymentQuotes.isEmpty());
    }

    @Test void failedOrderCreationReleasesReservationExactlyOnce() {
        ReqGenerateOrder req = new ReqGenerateOrder(); req.productId = "6003";
        assertTrue(service.generateOrderDetailInfo(player, req).success());
        assertEquals(1, state().dailyPurchases.get(6003));
        service.onOrderCreationFailed(player, req); service.onOrderCreationFailed(player, req);
        assertEquals(0, state().dailyPurchases.get(6003)); assertEquals(0, state().permanentPurchases.get(6003));
        assertTrue(state().paymentQuotes.isEmpty()); assertEquals(100, wallet.get(1024034));
    }

    @Test void rejectedSpecialRewardRefundsCostsAndRestoresQuota() {
        when(packs.addItems(eq(player.getId()), anyMap(), any(), anyString())).thenReturn(new CommonResult<>(Code.FAIL));
        var res = service.action(player, request(MiningConstant.EXCHANGE, 5001));
        assertEquals("SPECIAL_REWARD_FAILED_REFUNDED", res.reason);
        assertEquals(1000, wallet.get(1024037)); assertEquals(0, state().total.exchanges);
        assertFalse(state().dailyPurchases.containsKey(5001)); assertNull(state().delivery);
    }

    @Test void uncertainSpecialRewardBlocksRetriesUntilExplicitReconciliation() {
        when(packs.addItems(eq(player.getId()), anyMap(), any(), anyString())).thenThrow(new IllegalStateException("connection lost"));
        var res = service.action(player, request(MiningConstant.EXCHANGE, 5001));
        assertEquals(Code.EXCEPTION, res.code); assertNotNull(state().delivery); assertEquals(980, wallet.get(1024037));
        assertEquals("DELIVERY_REQUIRES_RECONCILIATION", service.action(player, request(MiningConstant.EXCHANGE, 5001)).reason);
        service.reconcileDelivery(player, state().delivery.id, false);
        assertNull(state().delivery); assertEquals(1000, wallet.get(1024037)); assertEquals(0, state().total.exchanges);
    }

    @Test void seasonResetPreservesLifetimeAchievementsInventoryAndRejectsOldVersion() {
        MiningState old = state(); old.total.grids = 100; old.claimedAchievements.add(1); old.depth = 20; saved = JSON.toJSONString(old);
        ReqMiningAction previous = request(MiningConstant.DIG, 101);
        MiningConfig.Season season = new MiningConfig.Season(); season.id = "s2";
        season.startTime = System.currentTimeMillis() - 1000; season.endTime = System.currentTimeMillis() + 100000;
        config.seasons = List.of(season);
        assertEquals("STALE_VERSION", service.action(player, previous).reason);
        assertEquals("s2", state().seasonId); assertEquals(0, state().depth); assertEquals(100, state().total.grids);
        assertTrue(state().claimedAchievements.contains(1)); assertEquals(100, wallet.get(1024034));
        verify(ranks).sync(eq(player.getId()), argThat(s -> "practice".equals(s.seasonId) && s.depth == 20));
    }
}
