package com.jjg.game.ploy.games.mining;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.core.constant.Code;
import com.jjg.game.ploy.games.mining.message.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MiningIntegrationContractTest {
    @BeforeAll static void setup() throws Exception { MiningFixtures.install(); }

    @Test void miningUsesPracticeDefaultsWithoutExternalJson() {
        MiningConfig config = new MiningConfig();
        assertTrue(config.enabled);
        assertTrue(config.permanentLimits.isEmpty());
        assertTrue(config.dailyTasks.isEmpty());
        assertEquals("practice", config.currentSeason(System.currentTimeMillis()).id);
        assertNotNull(JSON.parseObject("{\"usedAdTickets\":[\"legacy-ticket\"]}", MiningState.class));
    }

    @Test void requestsAndFullStateRoundTripThroughProductionSerializer() {
        ReqMiningAction request = new ReqMiningAction(); request.action = 1; request.id = 103; request.version = 123;
        request.seasonId = "s1"; request.row = 1000; request.column = 6; request.count = 1;
        ReqMiningAction copy = ProtostuffUtil.deserialize(ProtostuffUtil.serialize(request), ReqMiningAction.class);
        assertEquals(request.seasonId, copy.seasonId); assertEquals(123, copy.version); assertEquals(1000, copy.row);
        ResMiningState res = new ResMiningState(Code.SUCCESS); res.info = new MiningInfo(); res.scrollRows = 2;
        res.info.seasonId = "s1"; res.info.version = 124; res.info.nextPickRecoveryTime = 1800000600000L;
        MiningCellInfo cell = new MiningCellInfo(); cell.row = 1000; cell.column = 6; cell.hp = 1; cell.typeId = 1002;
        ItemInfo tool = new ItemInfo(); tool.itemId = 1024034; tool.count = 3;
        ItemInfo ore = new ItemInfo(); ore.itemId = 1024037; ore.count = 9;
        res.info.cells = List.of(cell); res.info.tools = List.of(tool); res.info.ores = List.of(ore);
        MiningRewardCellInfo rewardCell = new MiningRewardCellInfo(); rewardCell.row = 1000;
        rewardCell.column = 6; rewardCell.typeId = 1004; rewardCell.rewards = List.of(ore);
        res.rewardCells = List.of(rewardCell);
        ResMiningState result = ProtostuffUtil.deserialize(ProtostuffUtil.serialize(res), ResMiningState.class);
        assertEquals(Code.SUCCESS, result.code); assertEquals(1002, result.info.cells.getFirst().typeId); assertEquals(124, result.info.version);
        assertEquals(1800000600000L, result.info.nextPickRecoveryTime);
        assertEquals(2, result.scrollRows);
        assertEquals(1000, result.rewardCells.getFirst().row);
        assertEquals(1024037, result.rewardCells.getFirst().rewards.getFirst().itemId);
        assertEquals(1024034, result.info.tools.getFirst().itemId); assertEquals(1024037, result.info.ores.getFirst().itemId);

        MiningRankInfo rank = new MiningRankInfo(); rank.playerId = 1; rank.headFrameId = 2003;
        MiningRankInfo rankCopy = ProtostuffUtil.deserialize(ProtostuffUtil.serialize(rank), MiningRankInfo.class);
        assertEquals(2003, rankCopy.headFrameId);

        MiningBundleInfo bundle = new MiningBundleInfo(); bundle.nameLanguageId = 400800040;
        bundle.adCdEndTime = 1800000000000L;
        ItemInfo diamondCost = new ItemInfo(); diamondCost.itemId = 1980000; diamondCost.count = 600;
        bundle.cost = List.of(diamondCost);
        MiningBundleInfo bundleCopy = ProtostuffUtil.deserialize(ProtostuffUtil.serialize(bundle), MiningBundleInfo.class);
        assertEquals(400800040, bundleCopy.nameLanguageId);
        assertEquals(1800000000000L, bundleCopy.adCdEndTime);
        assertEquals(1980000, bundleCopy.cost.getFirst().itemId);
        assertEquals(600, bundleCopy.cost.getFirst().count);

        ResMiningExchangeShop shop = new ResMiningExchangeShop(Code.SUCCESS); shop.version = 125;
        MiningExchangeInfo good = new MiningExchangeInfo(); good.id = 5001; good.goods = List.of(tool); good.cost = List.of(ore);
        shop.goods = List.of(good); shop.currencies = List.of(ore);
        ResMiningExchangeShop shopCopy = ProtostuffUtil.deserialize(ProtostuffUtil.serialize(shop), ResMiningExchangeShop.class);
        assertEquals(125, shopCopy.version); assertEquals(5001, shopCopy.goods.getFirst().id);

        ResMiningAchievements achievements = new ResMiningAchievements(Code.SUCCESS);
        MiningTaskInfo task = new MiningTaskInfo(); task.id = 1;
        task.nameLanguageId = 400800052; task.descLanguageId = 400800057;
        achievements.achievements = List.of(task);
        ResMiningAchievements achievementsCopy = ProtostuffUtil.deserialize(
                ProtostuffUtil.serialize(achievements), ResMiningAchievements.class);
        assertEquals(400800052, achievementsCopy.achievements.getFirst().nameLanguageId);
        assertEquals(400800057, achievementsCopy.achievements.getFirst().descLanguageId);
    }

    @Test void seasonIntervalsAreExclusiveAndRankingRewardsComeFromExcel() {
        MiningConfig cfg = new MiningConfig(); MiningConfig.Season first = new MiningConfig.Season(); first.id = "s1";
        first.startTime = 100; first.endTime = 200;
        MiningConfig.Season second = new MiningConfig.Season(); second.id = "s2"; second.startTime = 200; second.endTime = 300;
        cfg.seasons = List.of(first, second);
        assertNull(cfg.currentSeason(99)); assertEquals("s1", cfg.currentSeason(100).id); assertEquals("s2", cfg.currentSeason(200).id);
        assertNull(cfg.currentSeason(300));
        Map<Integer, Long> firstReward = MiningRankService.reward(System.currentTimeMillis(), 1);
        assertEquals(4_000_000L, firstReward.get(1990000));
        assertEquals(50L, firstReward.get(1024013));
        assertTrue(MiningRankService.reward(System.currentTimeMillis(), 101).isEmpty());
    }
}
