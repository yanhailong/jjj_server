package com.jjg.game.core.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.baselogic.function.SystemInterfaceHolder;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.base.condition.MatchResult;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.base.drop.DropItemDao;
import com.jjg.game.core.base.drop.DropItemLogger;
import com.jjg.game.core.base.drop.ItemDropDataHolder;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.DropItemListener;
import com.jjg.game.core.pb.ActivityItemDropInfo;
import com.jjg.game.core.pb.NotifyItemDropInfo;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.MessageBuildUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.DropConfigCfg;
import com.jjg.game.sampledata.bean.PlayerLevelConfigCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ConditionManager conditionManager;
    private final ClusterSystem clusterSystem;
    private final GameFunctionService gameFunctionService;

    public DropItemManager(DropItemDao dropItemDao, ItemDropDataHolder itemDropDataHolder, PlayerPackService playerPackService, DropItemLogger dropItemLogger, ConditionManager conditionManager, ClusterSystem clusterSystem, GameFunctionService gameFunctionService) {
        this.dropItemDao = dropItemDao;
        this.itemDropDataHolder = itemDropDataHolder;
        this.playerPackService = playerPackService;
        this.dropItemLogger = dropItemLogger;
        this.conditionManager = conditionManager;
        this.clusterSystem = clusterSystem;
        this.gameFunctionService = gameFunctionService;
    }

    /**
     * 触发道具掉落
     */
    public Map<Integer, Long> triggerDropItem(Player player, AddType addType, String desc, int dropCfgId, int triggerTimes, PlayerEventCategory.PlayerEffectiveFlowingEvent event) {
        DropConfigCfg dropConfigCfg = GameDataManager.getDropConfigCfg(dropCfgId);
        if (dropConfigCfg == null) {
            log.error("不存在该掉落配置 playerId:{} dropCfgId:{} triggerTimes:{}", player.getId(), dropCfgId, triggerTimes);
            return Map.of();
        }
        Map<Integer, Integer> itemDropGroupCounter = dropItemDao.getItemDropGroupCounter(player.getId());
        if (itemDropGroupCounter == null) {
            itemDropGroupCounter = new HashMap<>();
        }
        Map<Integer, Long> dropItems = new HashMap<>();
        // 随机N次
        for (int i = 0; i < triggerTimes; i++) {
            // 获取当的掉落配置
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
                randDropItems.forEach(item ->
                        dropItems.merge(item.getSecond().getId(), item.getSecond().getItemCount(), Long::sum));
            }
        }
        if (dropItems.isEmpty()) {
            return Map.of();
        }
        // 更新道具掉落使用map
        dropItemDao.updateItemDropGroupCounter(player.getId(), itemDropGroupCounter);
        // 添加道具
        CommonResult<ItemOperationResult> result =
                playerPackService.addItems(player.getId(), dropItems, AddType.DROP_ITEM);
        if (result.success()) {
            // 记录日志
            dropItemLogger.recordDropItem(player, addType, desc, event.getGameCfgId(), dropItems, result.data);
        }
        return dropItems;
    }

    /**
     * 触发道具掉落
     *
     * @param player
     * @param addType
     * @param desc
     * @param dropTrunkId
     * @return
     */
    public Map<Integer, Long> triggerDropItem(Player player, long count, AddType addType, String desc, int dropTrunkId) {
        Map<Integer, Integer> itemDropGroupCounter = dropItemDao.getItemDropGroupCounter(player.getId());
        if (itemDropGroupCounter == null) {
            itemDropGroupCounter = new HashMap<>();
        }
        Map<Integer, Long> dropItems = new HashMap<>();
        for (int i = 0; i < count; i++) {
            Pair<Integer, Item> item = itemDropDataHolder.randDropItems(dropTrunkId, itemDropGroupCounter);
            if (item != null) {
                dropItems.merge(item.getSecond().getId(), item.getSecond().getItemCount(), Long::sum);
            }
        }
        if (dropItems.isEmpty()) {
            return dropItems;
        }
        // 更新道具掉落使用map
        dropItemDao.updateItemDropGroupCounter(player.getId(), itemDropGroupCounter);
        // 添加道具
        CommonResult<ItemOperationResult> result = playerPackService.addItems(player.getId(), dropItems, AddType.DROP_TRUNK_ITEM);
        if (result.success()) {
            // 记录日志
            dropItemLogger.recordDropItem(player, addType, desc, dropTrunkId, dropItems, result.data);
        }
        return dropItems;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        //赌场道具掉落
        if (gameEvent instanceof PlayerEventCategory.PlayerEffectiveFlowingEvent event) {
            Player player = event.getPlayer();
            //通知的掉落信息
            List<ActivityItemDropInfo> itemDropInfos = new ArrayList<>();
            //掉落监听
            List<DropItemListener> sysInterface = SystemInterfaceHolder.getGameSysInterface(DropItemListener.class);
            for (DropItemListener listener : sysInterface) {
                List<ActivityItemDropInfo> dropItem = listener.dropItem(player, event);
                if (CollectionUtil.isNotEmpty(dropItem)) {
                    itemDropInfos.addAll(dropItem);
                }
            }
            //赌场道具掉落
            List<ActivityItemDropInfo> dropItem = casinoDropItem(event);
            if (CollectionUtil.isNotEmpty(dropItem)) {
                itemDropInfos.addAll(dropItem);
            }
            // 如果有掉落
            if (!itemDropInfos.isEmpty()) {
                NotifyItemDropInfo notifyItemDropInfo = new NotifyItemDropInfo();
                notifyItemDropInfo.itemDropInfos = itemDropInfos;
                log.debug("玩家：{} 发送掉落数据：{}", player.getId(), JSON.toJSONString(notifyItemDropInfo));
                PFSession pfSession = clusterSystem.getSession(player.getId());
                if (pfSession != null) {
                    // 发送道具掉落信息
                    pfSession.send(notifyItemDropInfo);
                }
            }
        }
    }

    /**
     * @param event 流水事件
     * @return 掉落信息
     */
    private List<ActivityItemDropInfo> casinoDropItem(PlayerEventCategory.PlayerEffectiveFlowingEvent event) {
        Player player = event.getPlayer();
        //功能开放配置
        if (!gameFunctionService.checkGameFunctionOpen(player, EFunctionType.MY_CASINO, false)) {
            return List.of();
        }
        PlayerLevelConfigCfg cfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        if (cfg == null || cfg.getDropConfig() == 0) {
            return List.of();
        }
        DropConfigCfg dropConfigCfg = GameDataManager.getDropConfigCfg(cfg.getDropConfig());
        if (dropConfigCfg == null) {
            return List.of();
        }
        //暂时只有游戏流水条件
        BetEvent eventParam = BetEvent.getPlayerEffectiveParam(event);
        if (eventParam == null) {
            return List.of();
        }
        //获取多个条件下的最小触发次数
        int triggerTimes = 0;
        MatchResultData matchResultData = conditionManager.addProgressAndGetAchievements(player, eventParam, CountDao.CountType.MY_CASINO.getParam(), dropConfigCfg.getDropCondition());
        if (matchResultData.result() == MatchResult.MATCH) {
            triggerTimes = matchResultData.achieveTimes();
        }
        if (triggerTimes == 0) {
            return List.of();
        }
        //扣除积分
        Map<Integer, Long> dropItem = triggerDropItem(player, AddType.DROP_ITEM, player.getId() + "", dropConfigCfg.getId(), triggerTimes, event);
        if (CollectionUtil.isNotEmpty(dropItem)) {
            return List.of(MessageBuildUtil.buildActivityDropInfo(0, 0, event.getGameCfgId(), dropItem));
        }
        return List.of();
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.EFFECTIVE_FLOWING);
    }
}
