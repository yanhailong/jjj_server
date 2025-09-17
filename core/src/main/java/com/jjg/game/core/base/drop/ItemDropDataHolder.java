package com.jjg.game.core.base.drop;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.DropDetailedCfg;
import com.jjg.game.sampledata.bean.DropGroupCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

    private static final Logger log = LoggerFactory.getLogger(ItemDropDataHolder.class);
    // 分组ID <=> 子包随机权重+ <子包道具随机权重, 道具信息>
    private final Map<Integer, List<Pair<Integer, List<Pair<Integer, Item>>>>> dropGroupCfgMap = new HashMap<>();
    // 道具掉落分组限制map
    private final Map<Integer, Integer> dropGroupLimit = new HashMap<>();

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
        dropGroupCfgMap.clear();
        dropGroupLimit.clear();
        for (DropGroupCfg dropGroupCfg : GameDataManager.getDropGroupCfgList()) {
            // 缓存
            if (!dropGroupCfgMap.containsKey(dropGroupCfg.getTrunkID())) {
                dropGroupCfgMap.put(dropGroupCfg.getTrunkID(), new ArrayList<>());
            }
            dropGroupLimit.put(dropGroupCfg.getTrunkID(), dropGroupCfg.getDropCount());
            List<Pair<Integer, List<Pair<Integer, Item>>>> childGroups = dropGroupCfgMap.get(dropGroupCfg.getTrunkID());
            List<List<Integer>> dropDetailedId = dropGroupCfg.getDropDetailedID();
            for (List<Integer> dropGroupOfWeight : dropDetailedId) {
                if (dropGroupOfWeight.size() < 2) {
                    continue;
                }
                // 分组权重
                int groupWeight = dropGroupOfWeight.getFirst();
                int detailId = dropGroupOfWeight.get(1);
                DropDetailedCfg detailedCfg = GameDataManager.getDropDetailedCfg(detailId);
                List<Pair<Integer, Item>> dropItemDetailList = new ArrayList<>();
                if (detailedCfg == null) {
                    childGroups.add(new Pair<>(groupWeight, dropItemDetailList));
                    continue;
                }
                List<List<Integer>> detailedDropItem = detailedCfg.getDetailedDropItem();
                for (List<Integer> dropItemDetailed : detailedDropItem) {
                    if (dropItemDetailed.size() < 3) {
                        if (!dropItemDetailed.isEmpty()) {
                            // 道具随机掉落权重
                            int itemRandWeight = dropItemDetailed.getFirst();
                            dropItemDetailList.add(new Pair<>(itemRandWeight, new Item()));
                        }
                        continue;
                    }
                    // 道具随机掉落权重
                    int itemRandWeight = dropItemDetailed.getFirst();
                    Item item = new Item(dropItemDetailed.get(1), dropItemDetailed.get(2));
                    dropItemDetailList.add(new Pair<>(itemRandWeight, item));
                }
                childGroups.add(new Pair<>(groupWeight, dropItemDetailList));
            }
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
            // 分组已经掉落过的次数
            int usedDropGroupTimes = itemDropGroupCounter.get(dropGroupId);
            // 分组限制每天掉落的次数
            int limitedGroupTimes = getDropGroupLimit(dropGroupId);
            if (usedDropGroupTimes >= limitedGroupTimes) {
                continue;
            }
            List<Pair<Integer, List<Pair<Integer, Item>>>> itemDropGroupWeights = dropGroupCfgMap.get(dropGroupId);
            if (itemDropGroupWeights == null) {
                continue;
            }
            // 随机分组详情数据
            List<Pair<Integer, Item>> itemWeightMap = RandomUtils.randomByWeight(itemDropGroupWeights);
            // 如果是空的直接返回
            if (CollectionUtils.isEmpty(itemWeightMap)) {
                continue;
            }
            // 随机分组详情中的道具数据
            Item randedItem = RandomUtils.randomByWeight(itemWeightMap);
            // 如果随机到空的道具，继续
            if (randedItem.getId() == 0) {
                continue;
            }
            items.add(new Pair<>(dropGroupId, randedItem));
            // 更新掉落表次数
            itemDropGroupCounter.put(dropGroupId, usedDropGroupTimes + 1);
            log.info("通过分组ID：{} 随机到分组详情配置：{} 随机到道具：{}", dropGroupId, itemWeightMap, randedItem);
        }
        return items;
    }
}
