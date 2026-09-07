package com.jjg.game.ploy.games.mining;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.core.constant.Code;
import com.jjg.game.ploy.games.mining.message.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MiningIntegrationContractTest {
    @Test void jsonHotReloadUsesRealBeanPropertiesAndRetainsDefaults() {
        MiningConfig live = new MiningConfig();
        MiningConfig parsed = JSON.parseObject("{\"enabled\":false,\"dailyTasks\":[{\"id\":1,\"kind\":1,\"target\":50,\"nameLanguageId\":400800061,\"descLanguageId\":400800062,\"rewards\":{\"1024034\":5}}]}", MiningConfig.class);
        BeanUtils.copyProperties(parsed, live);
        assertFalse(live.enabled);
        assertEquals(5L, live.dailyTasks.getFirst().rewards.get(1024034));
        assertEquals(400800061, live.dailyTasks.getFirst().nameLanguageId);
        assertEquals(400800062, live.dailyTasks.getFirst().descLanguageId);
        assertNotNull(JSON.parseObject("{\"usedAdTickets\":[\"legacy-ticket\"]}", MiningState.class));
    }

    @Test void requestsAndFullStateRoundTripThroughProductionSerializer() {
        ReqMiningAction request = new ReqMiningAction(); request.action = 1; request.id = 103; request.version = 123;
        request.seasonId = "s1"; request.row = 1000; request.column = 6; request.count = 1;
        ReqMiningAction copy = ProtostuffUtil.deserialize(ProtostuffUtil.serialize(request), ReqMiningAction.class);
        assertEquals(request.seasonId, copy.seasonId); assertEquals(123, copy.version); assertEquals(1000, copy.row);
        ResMiningState res = new ResMiningState(Code.SUCCESS); res.info = new MiningInfo(); res.scrollRows = 2;
        res.info.seasonId = "s1"; res.info.version = 124;
        MiningCellInfo cell = new MiningCellInfo(); cell.row = 1000; cell.column = 6; cell.hp = 1; cell.typeId = 1002;
        ItemInfo tool = new ItemInfo(); tool.itemId = 1024034; tool.count = 3;
        ItemInfo ore = new ItemInfo(); ore.itemId = 1024037; ore.count = 9;
        res.info.cells = List.of(cell); res.info.tools = List.of(tool); res.info.ores = List.of(ore);
        MiningRewardCellInfo rewardCell = new MiningRewardCellInfo(); rewardCell.row = 1000;
        rewardCell.column = 6; rewardCell.typeId = 1004; rewardCell.rewards = List.of(ore);
        res.rewardCells = List.of(rewardCell);
        ResMiningState result = ProtostuffUtil.deserialize(ProtostuffUtil.serialize(res), ResMiningState.class);
        assertEquals(Code.SUCCESS, result.code); assertEquals(1002, result.info.cells.getFirst().typeId); assertEquals(124, result.info.version);
        assertEquals(2, result.scrollRows);
        assertEquals(1000, result.rewardCells.getFirst().row);
        assertEquals(1024037, result.rewardCells.getFirst().rewards.getFirst().itemId);
        assertEquals(1024034, result.info.tools.getFirst().itemId); assertEquals(1024037, result.info.ores.getFirst().itemId);

        MiningRankInfo rank = new MiningRankInfo(); rank.playerId = 1; rank.headFrameId = 2003;
        MiningRankInfo rankCopy = ProtostuffUtil.deserialize(ProtostuffUtil.serialize(rank), MiningRankInfo.class);
        assertEquals(2003, rankCopy.headFrameId);

        MiningBundleInfo bundle = new MiningBundleInfo(); bundle.nameLanguageId = 400800040;
        bundle.adCdEndTime = 1800000000000L;
        MiningBundleInfo bundleCopy = ProtostuffUtil.deserialize(ProtostuffUtil.serialize(bundle), MiningBundleInfo.class);
        assertEquals(400800040, bundleCopy.nameLanguageId);
        assertEquals(1800000000000L, bundleCopy.adCdEndTime);

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

    @Test void seasonIntervalsAreExclusiveAndRankingRewardRangesHaveBoundaries() {
        MiningConfig cfg = new MiningConfig(); MiningConfig.Season first = new MiningConfig.Season(); first.id = "s1";
        first.startTime = 100; first.endTime = 200;
        MiningConfig.Season second = new MiningConfig.Season(); second.id = "s2"; second.startTime = 200; second.endTime = 300;
        cfg.seasons = List.of(first, second);
        assertNull(cfg.currentSeason(99)); assertEquals("s1", cfg.currentSeason(100).id); assertEquals("s2", cfg.currentSeason(200).id);
        assertNull(cfg.currentSeason(300));
        MiningConfig.RankReward reward = new MiningConfig.RankReward(); reward.from = 1; reward.to = 300; reward.items = Map.of(1024034, 5L);
        second.rewards = List.of(reward);
        assertEquals(reward.items, MiningRankService.reward(second, 300)); assertTrue(MiningRankService.reward(second, 301).isEmpty());
    }
}
