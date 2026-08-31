package com.jjg.game.reddot;

import com.jjg.game.activity.adsReward.AdsRewardController;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.dao.PlayerActivityDao;
import com.jjg.game.activity.common.data.*;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.dailylogin.controller.DailyLoginController;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.activity.piggybank.controller.PiggyBankController;
import com.jjg.game.activity.piggybank.data.PiggyBankData;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.sampledata.bean.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityRedDotTest {
    private ActivityData activity(ActivityType type, long id) {
        ActivityData data = mock(ActivityData.class);
        when(data.getType()).thenReturn(type); when(data.getId()).thenReturn(id); when(data.canRun()).thenReturn(true);
        return data;
    }
    @Test void rewardVideoCountsEligibleUnclaimedTiersAndUsesToday() {
        AdsRewardController controller = spy(new AdsRewardController());
        ActivityData data = activity(ActivityType.ADS_REWARD, 10);
        when(data.getValue()).thenReturn(List.of(1, 2, 3));
        Map<Integer, VideoRewardCfg> configs = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            VideoRewardCfg cfg = mock(VideoRewardCfg.class);
            when(cfg.getId()).thenReturn(i); when(cfg.getVideoCount()).thenReturn(i);
            when(cfg.getRewards()).thenReturn(Map.of(100, 1L)); configs.put(i, cfg);
        }
        doReturn(configs).when(controller).getDetailCfgBean(data);
        CountDao countDao = mock(CountDao.class);
        when(countDao.getCount(anyString(), anyString())).thenReturn(java.math.BigDecimal.valueOf(3));
        PlayerActivityDao dao = mock(PlayerActivityDao.class);
        Map<Integer, PlayerActivityData> states = new HashMap<>();
        doReturn(states).when(dao).getPlayerActivityData(1, ActivityType.ADS_REWARD, 10);
        ReflectionTestUtils.setField(controller, "countDao", countDao);
        ReflectionTestUtils.setField(controller, "playerActivityDao", dao);
        assertEquals(3, controller.getRedDotCount(1, data));
        PlayerActivityData claimed = new PlayerActivityData(10, TimeHelper.getDayNumerical());
        claimed.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED); states.put(2, claimed);
        assertEquals(2, controller.getRedDotCount(1, data));
        claimed.setRound(0);
        assertEquals(3, controller.getRedDotCount(1, data));
        when(countDao.getCount(anyString(), anyString())).thenReturn(java.math.BigDecimal.ZERO);
        assertEquals(0, controller.getRedDotCount(1, data));
        verify(dao, never()).savePlayerActivityData(anyLong(), any(), anyLong(), anyMap());
    }

    @Test void piggyBankRequiresFullAndClaimableWithoutMutatingState() {
        PiggyBankController controller = spy(new PiggyBankController(null));
        ActivityData data = activity(ActivityType.PIGGY_BANK, 10);
        PiggyBankCfg cfg = mock(PiggyBankCfg.class);
        when(cfg.getFullUp()).thenReturn(100L); when(cfg.getBaseGold()).thenReturn(20);
        doReturn(Map.of(1, cfg, 2, cfg, 3, cfg)).when(controller).getDetailCfgBean(data);
        PiggyBankData full = new PiggyBankData(), partial = new PiggyBankData(), unbought = new PiggyBankData();
        full.setProgress(80); full.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
        partial.setProgress(79); partial.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
        unbought.setProgress(80);
        PlayerActivityDao dao = mock(PlayerActivityDao.class);
        doReturn(Map.of(1, full, 2, partial, 3, unbought)).when(dao).getPlayerActivityData(1, ActivityType.PIGGY_BANK, 10);
        ReflectionTestUtils.setField(controller, "playerActivityDao", dao);
        assertEquals(1, controller.getRedDotCount(1, data));
        assertEquals(ActivityConstant.ClaimStatus.CAN_CLAIM, partial.getClaimStatus());
        assertEquals(79, partial.getProgress());
    }

    @Test void dailyLoginCountsOnlyCurrentConfigClaimableRewards() {
        DailyLoginController controller = spy(new DailyLoginController(null));
        ActivityData data = activity(ActivityType.DAILY_LOGIN, 10);
        DailyRewardsCfg cfg = mock(DailyRewardsCfg.class); when(cfg.getGetItem()).thenReturn(Map.of(100, 1L));
        doReturn(Map.of(1, cfg, 2, cfg)).when(controller).getDetailCfgBean(data);
        PlayerActivityData claimable = new PlayerActivityData(); claimable.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
        PlayerActivityDao dao = mock(PlayerActivityDao.class);
        doReturn(Map.of(1, claimable, 2, claimable, 99, claimable)).when(dao).getPlayerActivityData(1, ActivityType.DAILY_LOGIN, 10);
        ReflectionTestUtils.setField(controller, "playerActivityDao", dao);
        Player player = mock(Player.class); CorePlayerService players = mock(CorePlayerService.class);
        when(players.get(1)).thenReturn(player);
        var conditions = mock(com.jjg.game.core.manager.ConditionManager.class);
        when(conditions.isAchievement(eq(player), eq(""), any(), isNull())).thenReturn(true);
        ReflectionTestUtils.setField(controller, "corePlayerService", players);
        ReflectionTestUtils.setField(controller, "conditionManager", conditions);
        ReflectionTestUtils.setField(controller, "activityManager", mock(ActivityManager.class));
        assertEquals(2, controller.getRedDotCount(1, data));
        claimable.setClaimStatus(ActivityConstant.ClaimStatus.NOT_CLAIM);
        when(cfg.getType()).thenReturn(ActivityConstant.DailyLogin.CONTINUE_TYPE);
        assertEquals(2, controller.getRedDotCount(1, data));
        assertEquals(ActivityConstant.ClaimStatus.NOT_CLAIM, claimable.getClaimStatus());
        when(conditions.isAchievement(eq(player), eq(""), any(), isNull())).thenReturn(false);
        assertEquals(0, controller.getRedDotCount(1, data));
    }

    @Test void aggregatesSameActivityTypeAndPushesThreeToTwoToZero() {
        ActivityManager manager = mock(ActivityManager.class, CALLS_REAL_METHODS);
        CorePlayerService players = mock(CorePlayerService.class);
        Player player = mock(Player.class); when(players.get(1)).thenReturn(player);
        RedDotManager dots = spy(new RedDotManager(null, null, null));
        doNothing().when(dots).updateRedDot(anyList(), anyLong());
        ReflectionTestUtils.setField(manager, "redDotPlayerService", players);
        ReflectionTestUtils.setField(manager, "redDotManager", dots);
        ActivityType type = ActivityType.ADS_REWARD;
        BaseActivityController previous = type.getController();
        BaseActivityController controller = mock(BaseActivityController.class, CALLS_REAL_METHODS);
        doReturn(true).when(controller).checkPlayerCanJoinActivity(eq(player), any());
        doReturn(RedDotDetails.RedDotType.COUNT).when(controller).getRedDotType();
        ActivityData first = activity(type, 10), second = activity(type, 11);
        ReflectionTestUtils.setField(controller, "activityManager", manager);
        ReflectionTestUtils.setField(controller, "redDotManager", dots);
        ReflectionTestUtils.setField(manager, "activityTypeData", Map.of(type, Map.of(10L, first, 11L, second)));
        ReflectionTestUtils.setField(type, "controller", controller);
        try {
            doReturn(2L).when(controller).getRedDotCount(1, first);
            doReturn(1L).when(controller).getRedDotCount(1, second);
            assertEquals(3, manager.initialize(1, 25).getFirst().getCount());
            doReturn(1L).when(controller).getRedDotCount(1, first);
            controller.updateRodDot(1, first, true, false);
            var captured = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(dots).updateRedDot(captured.capture(), eq(1L));
            assertEquals(2, ((RedDotDetails) captured.getValue().getFirst()).getCount());
            when(first.canRun()).thenReturn(false); when(second.canRun()).thenReturn(false);
            assertEquals(0, manager.initialize(1, 25).getFirst().getCount());
        } finally { ReflectionTestUtils.setField(type, "controller", previous); }
    }
}
