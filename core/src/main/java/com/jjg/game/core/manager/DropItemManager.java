package com.jjg.game.core.manager;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.base.drop.DropItemDao;
import com.jjg.game.core.base.drop.DropItemLogger;
import com.jjg.game.core.base.drop.ItemDropDataHolder;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.DropConfigCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/10/16 15:45
 */
@Component
public class DropItemManager implements GameEventListener {

    private final Logger log = LoggerFactory.getLogger(DropItemManager.class);
    private final DropItemDao dropItemDao;
    private final ItemDropDataHolder itemDropDataHolder;
    private final PlayerPackService playerPackService;
    private final DropItemLogger dropItemLogger;

    public DropItemManager(DropItemDao dropItemDao, ItemDropDataHolder itemDropDataHolder, PlayerPackService playerPackService, DropItemLogger dropItemLogger) {
        this.dropItemDao = dropItemDao;
        this.itemDropDataHolder = itemDropDataHolder;
        this.playerPackService = playerPackService;
        this.dropItemLogger = dropItemLogger;
    }

    /**
     * 触发道具掉落
     */
    public List<Item> triggerDropItem(Player player, String source, long sourceId, int dropCfgId, int triggerTimes, PlayerEventCategory.PlayerEffectiveFlowingEvent event) {
        DropConfigCfg dropConfigCfg = GameDataManager.getDropConfigCfg(dropCfgId);
        if (dropConfigCfg == null) {
            log.error("不存在该掉落配置 playerId:{} dropCfgId:{} triggerTimes:{}", player.getId(), dropCfgId, triggerTimes);
            return Collections.emptyList();
        }
        Map<Integer, Integer> itemDropGroupCounter = dropItemDao.getItemDropGroupCounter(player.getId());
        if (itemDropGroupCounter == null) {
            itemDropGroupCounter = new HashMap<>();
        }
        List<Item> dropItems = new ArrayList<>();
        // 随机N次
        for (int i = 0; i < triggerTimes; i++) {
            // 获取当前活动的掉落配置
            List<Integer> dropIdList = new ArrayList<>(dropConfigCfg.getDropId());
            Map<Integer, Integer> finalItemDropGroupCounter = itemDropGroupCounter;
            // 先排除已经不能掉落的分组ID
            dropIdList.removeIf(dropGroupId -> {
                int useTimes = finalItemDropGroupCounter.getOrDefault(dropGroupId, 0);
                int limitTimes = itemDropDataHolder.getDropGroupLimit(dropGroupId);
                return useTimes >= limitTimes;
            });
            // 根据分组配置，获取对应的子包组ID 分组ID <=> 道具ID
            List<Pair<Integer, Item>> randDropItems =
                    itemDropDataHolder.randDropItems(dropIdList, itemDropGroupCounter);
            if (!CollectionUtils.isEmpty(randDropItems)) {
                dropItems.addAll(randDropItems.stream().map(Pair::getSecond).toList());
            }
        }
        if (dropItems.isEmpty()) {
            return dropItems;
        }

        //将同一item.id的道具相加
        Map<Integer, Long> mergedMap = dropItems.stream()
                .collect(Collectors.groupingBy(
                        Item::getId,
                        Collectors.summingLong(Item::getItemCount)
                ));

        // 将 Map 转换回 List<Item>
        dropItems = mergedMap.entrySet().stream()
                .map(entry -> new Item(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // 更新道具掉落使用map
        dropItemDao.updateItemDropGroupCounter(player.getId(), itemDropGroupCounter);
        // 添加道具
        CommonResult<ItemOperationResult> result =
                playerPackService.addItems(player.getId(), dropItems, "DROP_ITEM");
        if (result.success()) {
            // 记录日志
            dropItemLogger.recordDropItem(player, source, sourceId, event.getGameCfgId(), dropItems, result.data);
        }
        return dropItems;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {

    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.EFFECTIVE_FLOWING);
    }
}
