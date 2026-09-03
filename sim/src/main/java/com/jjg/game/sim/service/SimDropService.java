package com.jjg.game.sim.service;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.DropItemCfg;
import com.jjg.game.sampledata.bean.DropNumCfg;
import com.jjg.game.sampledata.bean.DropTypeCfg;
import com.jjg.game.sampledata.bean.ItemCfg;
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
 * slots 旋转与 sim 联动: 扣能量 -> 加经验 -> 场景升级 -> 道具掉落 -> 入背包
 * <p>
 * 数据流参见 dropItem / dropType / dropNum / CasinoLevel 四张配置表。
 *
 * @author 11
 * @date 2026/6/5
 */
@Service
public class SimDropService {
    private static final Logger log = LoggerFactory.getLogger(SimDropService.class);

    @Autowired
    private SimConfigCacheService configCacheService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private SimGuideService guideService;

    /**
     * 玩家每次 slots 旋转触发 (运行在 hall 的 RPC 线程, 直接操作 ctx 内存数据)
     *
     * @param ctx       玩家 sim 上下文
     * @param gameType  slots 游戏类型 (如 SuperStar=100300)
     * @param winTimes  本次中奖倍数 (allWinGold / allBetScore)
     * @param enterType 进入方式: 0=普通, >0(赛季/拜访等)不扣能量
     */
    public CommonResult<SlotsSpinResult> onSpin(SimPlayerContext ctx, int gameType, int winTimes, SpinStatInfo statInfo, int enterType) {
        CommonResult<SlotsSpinResult> result = new CommonResult<>(Code.SUCCESS);

        SimBaseData base = ctx.getSimBaseData();
        SimCasinoData casino = ctx.getCurrentCasino();
        if (base == null || casino == null) {
            log.warn("slots 联动失败, 基础数据或当前场景为空 playerId={},gameType={}", ctx.playerId(), gameType);
            result.code = Code.FAIL;
            return result;
        }

        SlotsSpinResult slotsSpinResult = new SlotsSpinResult();

        //普通模式扣能量；特殊模式仅在最后一局掉落，不扣能量。
        if (enterType <= 0) {
            boolean normalMode = statInfo.getSpecialModes() != null && statInfo.getSpecialModes().contains(1);
            if (normalMode || statInfo.getRemainFreeCount() <= 0) {
                if (normalMode && base.getPower() < SimConstant.Common.SPIN_COST_POWER) {
                    guideService.triggerItemNotEnough(ctx, SimConstant.Item.ID_POWER);
                    log.info("能量不足, 跳过 slots 掉落联动 playerId={},gameType={},power={}", ctx.playerId(), gameType, base.getPower());
                    result.code = Code.FAIL;
                    return result;
                }
                int originalPower = base.getPower();
                int originalDropResetDay = base.getDropResetDay();
                Map<Integer, Integer> originalDropCount = base.getDailyDropCount() == null
                        ? null : new HashMap<>(base.getDailyDropCount());
                if (normalMode) {
                    base.setPower(base.getPower() - SimConstant.Common.SPIN_COST_POWER);
                }

                Map<Integer, Long> dropResult;
                try {
                    //掉落
                    dropResult = normalMode ? normalRollDrop(base, gameType, winTimes)
                            : specialRollDrop(base, gameType, winTimes);

                    //入账 (掉落可能含能量/知名度等特殊资源, 统一走 addItems 路由)
                    if (!dropResult.isEmpty()) {
                        CommonResult<ItemOperationResult> addResult = playerPackService.addItems(
                                ctx.playerId(), dropResult, AddType.SIM_SLOTS_DROP, null, false);
                        if (addResult == null || !addResult.success()) {
                            restoreSpinState(base, originalPower, originalDropResetDay, originalDropCount);
                            int code = addResult == null ? Code.FAIL : addResult.code;
                            log.info("slots 掉落失败 playerId={},gameType={},winTimes={},code={}",
                                    ctx.playerId(), gameType, winTimes, code);
                            result.code = code;
                            return result;
                        }
                    }
                } catch (RuntimeException e) {
                    restoreSpinState(base, originalPower, originalDropResetDay, originalDropCount);
                    throw e;
                }
                slotsSpinResult.setItemsMap(dropResult);
            }
        }

        slotsSpinResult.setPower(base.getPower());

        try {
            ItemCfg itemCfg = configCacheService.getResearchPointItemCfg(0);
            if (itemCfg != null) {
                slotsSpinResult.setResearchPoints((int) playerPackService.getItemCount(ctx.playerId(), itemCfg.getId()));
            }
        } catch (RuntimeException e) {
            log.warn("查询研究点失败 playerId={}", ctx.playerId(), e);
        }
        result.data = slotsSpinResult;
        return result;
    }

    private void restoreSpinState(SimBaseData base, int power, int dropResetDay, Map<Integer, Integer> dropCount) {
        base.setPower(power);
        base.setDropResetDay(dropResetDay);
        base.setDailyDropCount(dropCount);
    }

    /**
     * 掉落判定: 遍历该 gameType 的所有 dropItem 配置, 逐行独立判定。
     *
     * @return 掉落物品 itemId -> 数量 (可能为空)
     */
    private Map<Integer, Long> normalRollDrop(SimBaseData base, int gameType, int winTimes) {
        Map<Integer, DropItemCfg> cfgMap = configCacheService.getDropItemCfgMap(gameType);
        if (cfgMap == null || cfgMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Long> result = new HashMap<>();
        //跨天重置每日掉落计数
        base.checkResetDropCount(TimeHelper.getDayNumerical());

        for (DropItemCfg cfg : cfgMap.values()) {
            //小于1的表示特殊掉落
            if (cfg.getDropTypeProb() < 1) {
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
            if (typeCfg.getExecutionMode() != 1) {
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
     * 掉落判定: 遍历该 gameType 的所有 dropItem 配置, 逐行独立判定。
     *
     * @return 掉落物品 itemId -> 数量 (可能为空)
     */
    private Map<Integer, Long> specialRollDrop(SimBaseData base, int gameType, int winTimes) {
        Map<Integer, DropItemCfg> cfgMap = configCacheService.getDropItemCfgMap(gameType);
        if (cfgMap == null || cfgMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Long> result = new HashMap<>();
        //跨天重置每日掉落计数
        base.checkResetDropCount(TimeHelper.getDayNumerical());

        for (DropItemCfg cfg : cfgMap.values()) {
            //小于1的表示特殊掉落
            if (cfg.getDropTypeProb() > 0) {
                continue;
            }
            //a. 每日掉落次数限制
            if (cfg.getDropCount() > 0 && base.getDropCount(cfg.getId()) >= cfg.getDropCount()) {
                continue;
            }
            //b. 中奖倍数区间校验 [min,max] 闭区间
            DropTypeCfg typeCfg = GameDataManager.getDropTypeCfg(cfg.getDropType());
            if (typeCfg == null || typeCfg.getDropType() == null || typeCfg.getDropType().size() < 2) {
                log.warn("dropType 配置缺失或格式错误1 dropItemId={},dropType={}", cfg.getId(), cfg.getDropType());
                continue;
            }
            if (typeCfg.getExecutionMode() != 2) {
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
}
