package com.jjg.game.core.base.drop;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.DropDetailedCfg;
import com.jjg.game.sampledata.bean.DropGroupCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 道具掉落数据holder
 *
 * @author 2CL
 */
@Component
public class ItemDropDataHolder implements ConfigExcelChangeListener {

    private final Logger log = LoggerFactory.getLogger(ItemDropDataHolder.class);
    // 分组ID <=> 子包随机权重+ <子包道具随机权重, 道具信息>
    private Map<Integer, WeightRandom<WeightRandom<Item>>> dropGroupCfgMap = new HashMap<>();
    // 道具掉落分组限制map
    private Map<Integer, Integer> dropGroupLimit = new HashMap<>();

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(DropGroupCfg.EXCEL_NAME, this::reloadGameCacheData)
                .addChangeSampleFileObserveWithCallBack(DropGroupCfg.EXCEL_NAME, this::reloadGameCacheData)
                .addInitSampleFileObserveWithCallBack(DropDetailedCfg.EXCEL_NAME, this::reloadGameCacheData)
                .addChangeSampleFileObserveWithCallBack(DropDetailedCfg.EXCEL_NAME, this::reloadGameCacheData);
    }

    /**
     * 重载游戏缓存数据
     */
    private void reloadGameCacheData() {
        Map<Integer, WeightRandom<WeightRandom<Item>>> tempGroupCfgMap = new HashMap<>();
        Map<Integer, Integer> tempGroupLimitMap = new HashMap<>();
        for (DropGroupCfg dropGroupCfg : GameDataManager.getDropGroupCfgList()) {
            // 缓存
            if (!tempGroupCfgMap.containsKey(dropGroupCfg.getTrunkID())) {
                tempGroupCfgMap.put(dropGroupCfg.getTrunkID(), new WeightRandom<>());
            }
            tempGroupLimitMap.put(dropGroupCfg.getTrunkID(), dropGroupCfg.getDropCount());
            WeightRandom<WeightRandom<Item>> weightRandom = tempGroupCfgMap.get(dropGroupCfg.getTrunkID());
            List<List<Integer>> dropDetailedId = dropGroupCfg.getDropDetailedID();
            for (List<Integer> dropGroupOfWeight : dropDetailedId) {
                if (dropGroupOfWeight.size() < 2) {
                    continue;
                }
                // 分组权重
                int groupWeight = dropGroupOfWeight.getFirst();
                int detailId = dropGroupOfWeight.get(1);
                DropDetailedCfg detailedCfg = GameDataManager.getDropDetailedCfg(detailId);
                WeightRandom<Item> random = new WeightRandom<>();
                if (detailedCfg == null) {
                    weightRandom.add(random, groupWeight);
                    continue;
                }
                List<List<Integer>> detailedDropItem = detailedCfg.getDetailedDropItem();
                for (List<Integer> dropItemDetailed : detailedDropItem) {
                    if (dropItemDetailed.size() < 3) {
                        if (!dropItemDetailed.isEmpty()) {
                            // 道具随机掉落权重
                            int itemRandWeight = dropItemDetailed.getFirst();
                            random.add(new Item(), itemRandWeight);
                        }
                        continue;
                    }
                    // 道具随机掉落权重
                    int itemRandWeight = dropItemDetailed.getFirst();
                    Item item = new Item(dropItemDetailed.get(1), dropItemDetailed.get(2));
                    random.add(item, itemRandWeight);
                }
                weightRandom.add(random, groupWeight);
            }
        }
        if (!tempGroupLimitMap.isEmpty() && !tempGroupCfgMap.isEmpty()) {
            dropGroupCfgMap = tempGroupCfgMap;
            dropGroupLimit = tempGroupLimitMap;
        }
    }

    /**
     * 获取分组掉落限制
     */
    public Integer getDropGroupLimit(int dropGroupId) {
        return dropGroupLimit.getOrDefault(dropGroupId, Integer.MAX_VALUE);
    }

    /**
     * 获取掉落分组配置数据
     *
     * @param dropGroupIdList 掉落分组ID列表
     */
    public List<Pair<Integer, Item>> randDropItems(
            List<Integer> dropGroupIdList, Map<Integer, Integer> itemDropGroupCounter) {
        List<Pair<Integer, Item>> items = new ArrayList<>();
        for (Integer dropGroupId : dropGroupIdList) {
            Pair<Integer, Item> pair = randDropItems(dropGroupId, itemDropGroupCounter);
            if (pair == null) {
                continue;
            }
            items.add(pair);
        }
        return items;
    }

    /**
     * 获取掉落分组配置数据
     *
     * @param dropGroupId 掉落分组ID列表
     */
    public Pair<Integer, Item> randDropItems(
            int dropGroupId, Map<Integer, Integer> itemDropGroupCounter) {

        // 分组已经掉落过的次数
        int usedDropGroupTimes = itemDropGroupCounter.getOrDefault(dropGroupId, 0);
        // 分组限制每天掉落的次数
        int limitedGroupTimes = getDropGroupLimit(dropGroupId);
        if (usedDropGroupTimes >= limitedGroupTimes) {
            log.debug("已经掉落的次数大于限制 dropGroupId = {}, usedDropGroupTimes = {},limitedGroupTimes = {}", dropGroupId, usedDropGroupTimes, limitedGroupTimes);
            return null;
        }
        WeightRandom<WeightRandom<Item>> weightRandom = dropGroupCfgMap.get(dropGroupId);
        if (weightRandom == null) {
            log.debug("获取掉落道具失败 dropGroupId = {}", dropGroupId);
            return null;
        }
        // 随机分组详情数据
        WeightRandom<Item> itemWeight = weightRandom.next();
        // 如果是空的直接返回
        if (itemWeight == null) {
            return null;
        }
        // 随机分组详情中的道具数据
        Item randedItem = itemWeight.next();
        // 如果随机到空的道具，继续
        if (randedItem == null || randedItem.getId() == 0) {
            return null;
        }
        // 更新掉落表次数
        itemDropGroupCounter.put(dropGroupId, usedDropGroupTimes + 1);
        return new Pair<>(dropGroupId, randedItem);
    }

}
