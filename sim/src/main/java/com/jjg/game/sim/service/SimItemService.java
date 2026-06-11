package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sampledata.bean.DropItemCfg;
import com.jjg.game.sampledata.bean.DropNumCfg;
import com.jjg.game.sampledata.bean.DropTypeCfg;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * slots 旋转与 sim 联动: 扣能量 -> 加经验 -> 赌场升级 -> 道具掉落 -> 入背包
 * <p>
 * 数据流参见 dropItem / dropType / dropNum / CasinoLevel 四张配置表。
 *
 * @author 11
 * @date 2026/6/5
 */
@Service
public class SimItemService {
    private static final Logger log = LoggerFactory.getLogger(SimItemService.class);

    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private SimConfigCacheService configCacheService;

    /**
     * 玩家每次 slots 旋转触发 (运行在 hall 的 RPC 线程, 直接操作 ctx 内存数据)
     *
     * @param ctx      玩家 sim 上下文
     * @param gameType slots 游戏类型 (如 SuperStar=100300)
     * @param winTimes 本次中奖倍数 (allWinGold / allBetScore)
     */
    public CommonResult<SlotsSpinResult> onSpin(SimPlayerContext ctx, int gameType, int winTimes) {
        CommonResult<SlotsSpinResult> result = new CommonResult<>(Code.SUCCESS);

        SimBaseData base = ctx.getSimBaseData();
        SimCasinoData casino = ctx.getCurrentCasino();
        if (base == null || casino == null) {
            log.warn("slots 联动失败, 基础数据或当前赌场为空 playerId={},gameType={}", ctx.playerId(), gameType);
            result.code = Code.FAIL;
            return result;
        }

        //扣能量: 不足则跳过本次掉落联动 (不影响 slots 旋转本身)
        if (base.getPower() < SimConstant.Common.SPIN_COST_POWER) {
            log.info("能量不足, 跳过 slots 掉落联动 playerId={},gameType={},power={}", ctx.playerId(), gameType, base.getPower());
            result.code = Code.FAIL;
            return result;
        }
        base.setPower(base.getPower() - SimConstant.Common.SPIN_COST_POWER);
        //加经验
        casino.setExp(casino.getExp() + SimConstant.Common.SPIN_ADD_EXP);
        //升级检查
        checkLevelUp(casino);
        //掉落
        Map<Integer, Long> dropResult = rollDrop(base, gameType, winTimes);

        //入账 (掉落可能含能量/知名度等特殊资源, 统一走 addItems 路由)
        if (!dropResult.isEmpty()) {
            CommonResult<SimItemOperationResult> addResult = addItems(ctx, dropResult, AddType.SIM_SLOTS_DROP, null, false);
            if (!addResult.success()) {
                log.info("slots 掉落失败 playerId={},gameType={},winTimes={},code={}", ctx.playerId(), gameType, winTimes, addResult.code);
                result.code = addResult.code;
                return result;
            }
        }

        SlotsSpinResult slotsSpinResult = new SlotsSpinResult();
        slotsSpinResult.setItemsMap(dropResult);
        slotsSpinResult.setPower(base.getPower());
        result.data = slotsSpinResult;
        return result;
    }

    /**
     * 按累计经验 (exp) 与 CasinoLevel(RegionID=casinoId) 重算赌场等级。
     * levelUpExp 视为"达到该等级所需的累计经验", 取满足 exp>=levelUpExp 的最大等级 id。
     */
    private void checkLevelUp(SimCasinoData casino) {
        int nextLevel = casino.getCasinoLevel() + 1;
        CasinoStatsSheetCfg cfg = configCacheService.getCasinoStatsSheetCfg(casino.getCasinoId(), nextLevel);
        if (cfg == null) {
            return;
        }
        if (casino.getExp() < cfg.getUpgradeCost()) {
            return;
        }
        casino.setCasinoLevel(nextLevel);
        casino.setExp(casino.getExp() - cfg.getUpgradeCost());
        log.info("赌场升级 playerId={},casinoId={},newLevel={}", casino.getPlayerId(), casino.getCasinoId(), nextLevel);
    }

    /**
     * 掉落判定: 遍历该 gameType 的所有 dropItem 配置, 逐行独立判定。
     *
     * @return 掉落物品 itemId -> 数量 (可能为空)
     */
    private Map<Integer, Long> rollDrop(SimBaseData base, int gameType, int winTimes) {
        Map<Integer, Long> result = new HashMap<>();
        //跨天重置每日掉落计数
        base.checkResetDropCount(TimeHelper.getDayNumerical());

        for (DropItemCfg cfg : GameDataManager.getDropItemCfgList()) {
            if (cfg.getGameType() != gameType) {
                continue;
            }
            //a. 每日掉落次数限制
            if (cfg.getDropCount() > 0 && base.getDropCount(cfg.getId()) >= cfg.getDropCount()) {
                continue;
            }
            //b. 掉落概率 (万分比)
            if (!RandomUtils.getRandomBoolean10000(cfg.getDropTypeProb())) {
                continue;
            }
            //c. 中奖倍数区间校验 [min,max] 闭区间
            DropTypeCfg typeCfg = GameDataManager.getDropTypeCfg(cfg.getDropType());
            if (typeCfg == null || typeCfg.getDropType() == null || typeCfg.getDropType().size() < 2) {
                log.warn("dropType 配置缺失或格式错误 dropItemId={},dropType={}", cfg.getId(), cfg.getDropType());
                continue;
            }
            int min = typeCfg.getDropType().get(0);
            int max = typeCfg.getDropType().get(1);
            if (winTimes < min || winTimes > max) {
                continue;
            }
            //d. 按权重选 (dropNumId, 随机次数)
            List<Integer> row = pickRowByWeight(cfg.getDropItem());
            if (row == null || row.size() < 3) {
                log.warn("dropItem.DropItem 配置格式错误 dropItemId={}", cfg.getId());
                continue;
            }
            int dropNumId = row.get(1);
            int repeat = row.get(2);
            DropNumCfg numCfg = GameDataManager.getDropNumCfg(dropNumId);
            if (numCfg == null || numCfg.getDetailedDropItem() == null || numCfg.getDetailedDropItem().isEmpty()) {
                log.warn("dropNum 配置缺失 dropItemId={},dropNumId={}", cfg.getId(), dropNumId);
                continue;
            }
            //e. 循环 repeat 次, 每次按权重抽 (道具id, 数量)
            for (int i = 0; i < repeat; i++) {
                List<Integer> detail = pickRowByWeight(numCfg.getDetailedDropItem());
                if (detail == null || detail.size() < 3) {
                    continue;
                }
                result.merge(detail.get(1), (long) detail.get(2), Long::sum);
            }
            base.addDropCount(cfg.getId());
        }
        return result;
    }

    /**
     * 按权重抽取一整行 (配置格式: [[权重, 值1, 值2...], ...], 以每行第 0 个元素为权重)。
     * 复用现有 RandomUtils.randomByWeightList 只能拿到 index1, 这里需要整行故单独实现。
     *
     * @return 命中的整行 (含权重); 无有效权重返回 null
     */
    private List<Integer> pickRowByWeight(List<List<Integer>> weightList) {
        if (weightList == null || weightList.isEmpty()) {
            return null;
        }
        int total = 0;
        for (List<Integer> row : weightList) {
            if (row != null && !row.isEmpty()) {
                total += row.get(0);
            }
        }
        if (total <= 0) {
            return null;
        }
        int rand = RandomUtils.randomInt(total);
        int acc = 0;
        for (List<Integer> row : weightList) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            acc += row.get(0);
            if (rand < acc) {
                return row;
            }
        }
        return weightList.get(weightList.size() - 1);
    }

    /**
     * 添加道具 (BuildingOutputType 维度): 转换为 itemId 后统一入账
     */
    public CommonResult<SimItemOperationResult> addItem(SimPlayerContext ctx, Map<BuildingOutputType, Long> resources, AddType addType, String desc, boolean notify) {
        return addItems(ctx, toItemMap(resources), addType, desc, notify);
    }

    /**
     * 添加道具 (itemId 维度统一入口)
     * <p>
     * 能量(ID_POWER)/知名度(ID_AWARENESS) 等 sim 特殊资源不进背包, 累加到玩家对应字段; 其余道具入背包。
     */
    public CommonResult<SimItemOperationResult> addItems(SimPlayerContext ctx, Map<Integer, Long> items, AddType addType, String desc, boolean notify) {
        CommonResult<SimItemOperationResult> result = new CommonResult<>(Code.SUCCESS);
        if (items == null || items.isEmpty()) {
            result.code = Code.FAIL;
            return result;
        }
        Map<Integer, Long> packItems = new HashMap<>(items.size());
        for (Map.Entry<Integer, Long> en : items.entrySet()) {
            int itemId = en.getKey();
            long count = en.getValue();
            if (count <= 0) {
                continue;
            }
            if (itemId == SimConstant.Item.ID_POWER) {
                SimBaseData base = ctx.getSimBaseData();
                base.setPower(base.getPower() + (int) count);
            } else if (itemId == SimConstant.Item.ID_AWARENESS) {
                SimCasinoData casino = ctx.getCurrentCasino();
                if (casino != null) {
                    casino.setAwareness(casino.getAwareness() + (int) count);
                }
            } else {
                packItems.merge(itemId, count, Long::sum);
            }
        }

        SimItemOperationResult data = new SimItemOperationResult();
        if (!packItems.isEmpty()) {
            CommonResult<ItemOperationResult> itemResult = playerPackService.addItems(ctx.playerId(), packItems, addType, desc, notify);
            if (itemResult.success() && itemResult.data != null) {
                data = SimItemOperationResult.createFromItemResult(itemResult.data);
            }
            log.info("道具入账 playerId={},items={},addType={},code={}", ctx.playerId(), packItems, addType,itemResult.code);
        }
        //回填 sim 特殊资源最新值, 供下发客户端
        data.setPower(ctx.getSimBaseData().getPower());
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino != null) {
            data.setAwareness(casino.getAwareness());
        }
        result.data = data;
        return result;
    }

    /**
     * 扣除道具 (itemId 维度统一入口): 先校验充足再扣除, 任一不足整体失败且不产生扣除。
     *
     * @return 扣除成功返回 true
     */
    public boolean removeItems(SimPlayerContext ctx, Map<Integer, Long> items, AddType addType, String desc) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        long needPower = 0;
        long needAwareness = 0;
        Map<Integer, Long> packItems = new HashMap<>(items.size());
        for (Map.Entry<Integer, Long> en : items.entrySet()) {
            int itemId = en.getKey();
            long count = en.getValue();
            if (count <= 0) {
                continue;
            }
            if (itemId == SimConstant.Item.ID_POWER) {
                needPower += count;
            } else if (itemId == SimConstant.Item.ID_AWARENESS) {
                needAwareness += count;
            } else {
                packItems.merge(itemId, count, Long::sum);
            }
        }

        SimBaseData base = ctx.getSimBaseData();
        SimCasinoData casino = ctx.getCurrentCasino();
        //先校验特殊资源是否充足
        if (needPower > 0 && base.getPower() < needPower) {
            log.warn("扣除道具失败, 能量不足 playerId={},need={},have={}", ctx.playerId(), needPower, base.getPower());
            return false;
        }
        if (needAwareness > 0 && (casino == null || casino.getAwareness() < needAwareness)) {
            log.warn("扣除道具失败, 知名度不足 playerId={},need={},have={}", ctx.playerId(), needAwareness, casino == null ? 0 : casino.getAwareness());
            return false;
        }
        //背包道具扣除 (失败不产生副作用, 此时特殊资源尚未扣除)
        if (!packItems.isEmpty()) {
            CommonResult<ItemOperationResult> r = playerPackService.removeItems(ctx.getPlayerController().getPlayer(), packItems, addType, desc);
            if (!r.success()) {
                log.warn("扣除道具失败 playerId={},items={},code={}", ctx.playerId(), packItems, r.code);
                return false;
            }
        }
        //背包扣除成功后再扣除特殊资源
        if (needPower > 0) {
            base.setPower(base.getPower() - (int) needPower);
        }
        if (needAwareness > 0) {
            casino.setAwareness(casino.getAwareness() - (int) needAwareness);
        }
        return true;
    }

    /**
     * 扣除单个道具
     */
    public boolean removeItem(SimPlayerContext ctx, int itemId, long count, AddType addType) {
        if (count <= 0) {
            return true;
        }
        return removeItems(ctx, Collections.singletonMap(itemId, count), addType, null);
    }

    /**
     * BuildingOutputType 产出映射为 itemId (无对应道具的产出类型返回 null, 不入账)
     */
    private Map<Integer, Long> toItemMap(Map<BuildingOutputType, Long> resources) {
        if (resources == null || resources.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Long> items = new HashMap<>(resources.size());
        for (Map.Entry<BuildingOutputType, Long> en : resources.entrySet()) {
            Integer itemId = toItemId(en.getKey());
            if (itemId == null) {
                continue;
            }
            items.merge(itemId, en.getValue(), Long::sum);
        }
        return items;
    }

    private Integer toItemId(BuildingOutputType type) {
        return switch (type) {
            case GOLD -> ItemUtils.getGoldItemId();
            case POWER -> SimConstant.Item.ID_POWER;
            case AWARENESS -> SimConstant.Item.ID_AWARENESS;
            default -> null;
        };
    }
}
