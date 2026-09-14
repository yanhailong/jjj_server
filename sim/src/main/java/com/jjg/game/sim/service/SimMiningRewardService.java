package com.jjg.game.sim.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;

/** 在玩家 SIM owner 线程调用；与 AllianceShopService 使用同一个 PlayerPackService 发奖。 */
@Service
public class SimMiningRewardService {
    private final SimPlayerContextRegistry contexts;
    private final SimConfigCacheService configs;
    private final PlayerPackService packs;

    public SimMiningRewardService(SimPlayerContextRegistry contexts, SimConfigCacheService configs, PlayerPackService packs) {
        this.contexts = contexts; this.configs = configs; this.packs = packs;
    }

    public CommonResult<ItemOperationResult> grant(long playerId, Map<Integer, Long> rewards, AddType source, String deliveryId) {
        SimPlayerContext ctx = contexts.getContext(playerId);
        if (ctx == null) return new CommonResult<>(Code.NOT_FOUND);
        if (rewards == null || rewards.size() != 1 || deliveryId == null || deliveryId.isBlank())
            return new CommonResult<>(Code.PARAM_ERROR);
        var reward = rewards.entrySet().iterator().next();
        if (reward.getKey() == null || reward.getValue() == null || reward.getValue() <= 0)
            return new CommonResult<>(Code.PARAM_ERROR);
        ItemCfg item = GameDataManager.getItemCfg(reward.getKey());
        if (item == null) return new CommonResult<>(Code.SAMPLE_ERROR);
        if (item.getItemType() == GameConstant.Item.ITEM_TYPE_SIM_GUEST) {
            if (ctx.getCurrentCasino() == null) return new CommonResult<>(Code.NOT_FOUND);
            if (configs.getVisitorQuestCfgByItemId(item.getId()) == null) return new CommonResult<>(Code.SAMPLE_ERROR);
        } else if (item.getItemType() == GameConstant.Item.ITEM_TYPE_SIM_EMPLOYEE) {
            if (configs.getEmployeeProfileCfgByItemId(item.getId()) == null) return new CommonResult<>(Code.SAMPLE_ERROR);
        } else return new CommonResult<>(Code.PARAM_ERROR);

        // 必须在大厅调用：PLOY 无 SpecialItemListener，会跳过非背包角色并返回假成功。
        CommonResult<ItemOperationResult> result = packs.addItems(playerId, rewards, source, "mining:delivery:" + deliveryId, true);
        if (result == null || !result.success()) {
            // 进入发奖之后的异常不能宣称没有副作用，交给原有挖矿 pending 核账流程。
            return new CommonResult<>(Code.EXCEPTION);
        }
        ctx.setLastSaveTime(0);
        return result;
    }
}
