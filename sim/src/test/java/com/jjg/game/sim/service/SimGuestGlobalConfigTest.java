package com.jjg.game.sim.service;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.VisitorTargetListCfg;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.res.ResSpecialGuestList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.jjg.game.sim.constant.SimConstant.Global.*;
import static com.jjg.game.sim.constant.SimConstant.SpecialGuest.POOL_PAID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimGuestGlobalConfigTest {
    private final SimConfigCacheService cache = new SimConfigCacheService(null, null);
    private final SimGuestService service = new SimGuestService();
    private final VisitorTargetListCfg pool = mock(VisitorTargetListCfg.class);
    private final SimPlayerContext ctx = mock(SimPlayerContext.class);
    private final SimCasinoData casino = new SimCasinoData();
    private final PlayerPackService pack = mock(PlayerPackService.class);
    private GlobalConfigCfg hours;
    private GlobalConfigCfg costs;
    private GlobalConfigCfg refreshLimit;
    private MockedStatic<GameDataManager> data;

    @BeforeEach
    void setUp() {
        data = mockStatic(GameDataManager.class);
        hours = global(SPECIAL_GUEST_DAILY_REFRESH_TIME, "0_6_10_14_19", 0);
        costs = global(SPECIAL_GUEST_REFRESH_COST, "1980000_0|1980000_20|1980000_50", 0);
        refreshLimit = global(SPECIAL_GUEST_MAX_MANUAL_REFRESH_PER_PERIOD, "999", 4);
        global(SPECIAL_GUEST_MAX_PURCHASE_PER_REFRESH, "999", 1);
        reload();
        when(pool.getId()).thenReturn(20);
        when(pool.getRegionID()).thenReturn(1);
        when(pool.getPoolType()).thenReturn(POOL_PAID);
        when(pool.getIsEnabled()).thenReturn(true);
        when(pool.getIsRefreshByTimePeriod()).thenReturn(true);
        when(pool.getManualRefresh()).thenReturn(true);
        data.when(GameDataManager::getVisitorTargetListCfgList).thenReturn(List.of(pool));
        ReflectionTestUtils.invokeMethod(cache, "loadVisitorTargetListConfig");
        casino.setCasinoId(1);
        casino.setSpecialGuestPaidCfgIds(new ArrayList<>());
        casino.setSpecialGuestOfferVersion(4);
        casino.setSpecialGuestNextRefreshTime(Long.MAX_VALUE);
        when(ctx.getCurrentCasino()).thenReturn(casino);
        when(ctx.getSimBaseData()).thenReturn(new SimBaseData());
        when(ctx.getPlayer()).thenReturn(new Player());
        ReflectionTestUtils.setField(service, "configCache", cache);
        ReflectionTestUtils.setField(service, "playerPackService", pack);
        ReflectionTestUtils.setField(service, "redDotManager", mock(RedDotManager.class));
    }

    @AfterEach
    void close() {
        data.close();
    }

    @ParameterizedTest
    @CsvSource({"0,6,0", "5,6,0", "6,10,0", "10,14,0", "18,19,0", "19,0,1", "23,0,1"})
    void usesGlobalHoursAtAndBetweenRefreshBoundaries(int nowHour, int nextHour, int days) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 15, nowHour, 0);
        long actual = ReflectionTestUtils.invokeMethod(service, "nextSpecialGuestRefreshTime", pool, millis(now));
        assertEquals(millis(now.toLocalDate().plusDays(days).atTime(nextHour, 0)), actual);
    }

    @Test
    void unorderedDuplicateHoursAndDisabledTimedRefreshKeepTheirSemantics() {
        when(hours.getValue()).thenReturn("19_0_6_6");
        reload();
        LocalDateTime now = LocalDateTime.of(2026, 9, 15, 6, 0);
        assertEquals(millis(now.withHour(19)), (long) ReflectionTestUtils.invokeMethod(service, "nextSpecialGuestRefreshTime", pool, millis(now)));
        when(pool.getIsRefreshByTimePeriod()).thenReturn(false);
        assertEquals(0L, (long) ReflectionTestUtils.invokeMethod(service, "nextSpecialGuestRefreshTime", pool, millis(now)));
    }

    @ParameterizedTest
    @CsvSource({"0,0", "1,20", "2,50", "3,50", "100,50"})
    void refreshCostsUseGlobalTiersAndRepeatTheLastTier(int refreshCount, long expected) {
        ItemInfo cost = ReflectionTestUtils.invokeMethod(service, "getSpecialGuestRefreshCost", refreshCount);
        assertEquals(1980000, cost.itemId);
        assertEquals(expected, cost.count);
    }

    @Test
    void configurationIsParsedOnceAndReloadReplacesTheCachedValues() {
        List<Integer> oldHours = cache.getSpecialGuestDailyRefreshHours();
        assertSame(oldHours, cache.getSpecialGuestDailyRefreshHours());
        when(hours.getValue()).thenReturn("2_8");
        when(costs.getValue()).thenReturn("1980001_7");
        assertEquals(List.of(0, 6, 10, 14, 19), cache.getSpecialGuestDailyRefreshHours());
        reload();
        assertEquals(List.of(2, 8), cache.getSpecialGuestDailyRefreshHours());
        assertEquals(List.of(0, 6, 10, 14, 19), oldHours);
        ItemInfo cost = ReflectionTestUtils.invokeMethod(service, "getSpecialGuestRefreshCost", 5);
        assertEquals(1980001, cost.itemId);
        assertEquals(7, cost.count);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "24", "-1", "6_", "6_bad"})
    void invalidHoursDoNotReplaceTheLastValidConfiguration(String value) {
        List<Integer> previous = cache.getSpecialGuestDailyRefreshHours();
        when(hours.getValue()).thenReturn(value);
        assertThrows(IllegalArgumentException.class, this::reload);
        assertSame(previous, cache.getSpecialGuestDailyRefreshHours());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1980000", "0_20", "1980000_-1", "1980000_20|", "1980000_20_1"})
    void invalidCostsDoNotPublishPartiallyLoadedHours(String value) {
        List<Integer> previous = cache.getSpecialGuestDailyRefreshHours();
        when(hours.getValue()).thenReturn("8");
        when(costs.getValue()).thenReturn(value);
        assertThrows(IllegalArgumentException.class, this::reload);
        assertSame(previous, cache.getSpecialGuestDailyRefreshHours());
        assertEquals(3, cache.getSpecialGuestRefreshCosts().size());
    }

    @Test
    void manualRefreshChargesCurrentTierClearsOfferCountsAndReturnsNextTier() {
        casino.setSpecialGuestRefreshCount(1);
        casino.getSpecialGuestPurchaseCounts().put("20:101", 1);
        when(pack.removeItem(ctx.getPlayer(), 1980000, 20, AddType.SIM_SPECIAL_GUEST_REFRESH)).thenReturn(new CommonResult<>(Code.SUCCESS));
        ResSpecialGuestList response = refresh();
        assertEquals(Code.SUCCESS, response.code);
        assertEquals(2, response.refreshCount);
        assertEquals(50, response.nextRefreshCost.count);
        assertEquals(4, response.maxManualRefreshPerPeriod);
        assertTrue(casino.getSpecialGuestPurchaseCounts().isEmpty());
        assertEquals(5, casino.getSpecialGuestOfferVersion());
        assertEquals(Long.MAX_VALUE, casino.getSpecialGuestNextRefreshTime());
        verify(pack).removeItem(ctx.getPlayer(), 1980000, 20, AddType.SIM_SPECIAL_GUEST_REFRESH);
    }

    @Test
    void globalManualLimitRejectsBeforeChargingOrChangingTheOffer() {
        casino.setSpecialGuestRefreshCount(4);
        casino.getSpecialGuestPurchaseCounts().put("20:101", 1);
        assertEquals(Code.REFRESH_MAX, refresh().code);
        assertEquals(4, casino.getSpecialGuestOfferVersion());
        assertEquals(1, casino.getSpecialGuestPurchaseCounts().get("20:101"));
        verifyNoInteractions(pack);
    }

    @Test
    void failedRefreshPaymentKeepsTheCurrentOfferAndCounters() {
        casino.setSpecialGuestRefreshCount(1);
        casino.getSpecialGuestPurchaseCounts().put("20:101", 1);
        when(pack.removeItem(ctx.getPlayer(), 1980000, 20, AddType.SIM_SPECIAL_GUEST_REFRESH)).thenReturn(new CommonResult<>(Code.FAIL));
        assertEquals(Code.FAIL, refresh().code);
        assertEquals(1, casino.getSpecialGuestRefreshCount());
        assertEquals(4, casino.getSpecialGuestOfferVersion());
        assertEquals(1, casino.getSpecialGuestPurchaseCounts().get("20:101"));
    }

    @Test
    void freeRefreshRedDotUsesTheSameGlobalCostAndLimit() {
        long now = System.currentTimeMillis();
        assertEquals(true, ReflectionTestUtils.invokeMethod(service, "hasFreeSpecialGuestRefresh", casino, now));
        casino.setSpecialGuestRefreshCount(1);
        assertEquals(false, ReflectionTestUtils.invokeMethod(service, "hasFreeSpecialGuestRefresh", casino, now));
        casino.setSpecialGuestNextRefreshTime(now);
        assertEquals(true, ReflectionTestUtils.invokeMethod(service, "hasFreeSpecialGuestRefresh", casino, now));
        casino.setSpecialGuestNextRefreshTime(Long.MAX_VALUE);
        when(costs.getValue()).thenReturn("1980000_0");
        reload();
        casino.setSpecialGuestRefreshCount(4);
        assertEquals(false, ReflectionTestUtils.invokeMethod(service, "hasFreeSpecialGuestRefresh", casino, now));
        when(refreshLimit.getIntValue()).thenReturn(0);
        assertEquals(true, ReflectionTestUtils.invokeMethod(service, "hasFreeSpecialGuestRefresh", casino, now));
    }

    @Test
    void globalChangeCallbackReloadsTheParsedConfiguration() {
        var callbacks = ConfigExcelChangeListener.getChangeCallbackCollector();
        var previous = callbacks.put(GlobalConfigCfg.EXCEL_NAME, new ArrayList<>());
        try {
            cache.changeSampleCallbackCollector();
            when(hours.getValue()).thenReturn("3_9");
            when(costs.getValue()).thenReturn("1980000_8");
            var globalCallbacks = callbacks.get(GlobalConfigCfg.EXCEL_NAME);
            assertEquals(1, globalCallbacks.size());
            globalCallbacks.getFirst().run();
            assertEquals(List.of(3, 9), cache.getSpecialGuestDailyRefreshHours());
            assertEquals(8, cache.getSpecialGuestRefreshCosts().getFirst().getItemCount());
        } finally {
            if (previous == null) {
                callbacks.remove(GlobalConfigCfg.EXCEL_NAME);
            } else {
                callbacks.put(GlobalConfigCfg.EXCEL_NAME, previous);
            }
        }
    }

    private GlobalConfigCfg global(int id, String value, int intValue) {
        GlobalConfigCfg cfg = mock(GlobalConfigCfg.class);
        when(cfg.getValue()).thenReturn(value);
        when(cfg.getIntValue()).thenReturn(intValue);
        data.when(() -> GameDataManager.getGlobalConfigCfg(id)).thenReturn(cfg);
        return cfg;
    }

    private void reload() {
        ReflectionTestUtils.invokeMethod(cache, "loadSpecialGuestGlobalConfig");
    }

    private ResSpecialGuestList refresh() {
        service.refreshSpecialGuests(ctx);
        ArgumentCaptor<ResSpecialGuestList> response = ArgumentCaptor.forClass(ResSpecialGuestList.class);
        verify(ctx).send(response.capture());
        return response.getValue();
    }

    private long millis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
