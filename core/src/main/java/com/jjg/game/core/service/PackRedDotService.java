package com.jjg.game.core.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.listener.ItemAddListener;
import com.jjg.game.core.listener.ItemConsumeListener;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.*;

/** 礼包页签数字红点：按可使用礼包所在格子计数，堆叠数量不重复计入。 */
@Service
public class PackRedDotService implements IRedDotService, ItemAddListener, ItemConsumeListener {
    @Lazy @Autowired private PlayerPackService packs;
    @Autowired private RedDotManager manager;
    @Override public RedDotDetails.RedDotModule getModule() { return RedDotDetails.RedDotModule.PACK; }
    @Override public List<Integer> getSubmodules() { return List.of(0, 1); }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        if (submodule != 0 && submodule != 1) return List.of();
        PlayerPack pack = packs.getFromAllDB(playerId);
        List<Integer> grids = new ArrayList<>();
        if (pack != null && pack.getItems() != null) {
            pack.getItems().forEach((grid, item) -> {
                if (item != null && item.getItemCount() > 0 && isUsableGift(GameDataManager.getItemCfg(item.getId()))) grids.add(grid);
            });
        }
        Collections.sort(grids);
        RedDotDetails dot = manager.buildRedDotDetails(getModule(), 1, grids.size());
        dot.setExtra(JSON.toJSONString(Map.of("gridIds", grids)));
        return List.of(dot);
    }

    static boolean isUsableGift(ItemCfg cfg) {
        return cfg != null && cfg.getType() == GameConstant.Item.TYPE_CAN_USE
                && ((cfg.getGetItem() != null && !cfg.getGetItem().isEmpty())
                || (cfg.getSelectGetItem() != null && !cfg.getSelectGetItem().isEmpty()) || cfg.getDropId() > 0);
    }

    public void refresh(long playerId) {
        try { manager.updateRedDot(initialize(playerId, 0), playerId); }
        catch (Exception e) { org.slf4j.LoggerFactory.getLogger(getClass()).error("刷新背包红点失败 playerId={}", playerId, e); }
    }
    @Override public void onItemsAdded(long id, Map<Integer, Long> items, AddType type) { refreshIfRelevant(id, items); }
    @Override public void onItemsConsumed(long id, Map<Integer, Long> items, AddType type) { refreshIfRelevant(id, items); }
    private void refreshIfRelevant(long id, Map<Integer, Long> items) {
        if (items != null && items.keySet().stream().anyMatch(itemId -> isUsableGift(GameDataManager.getItemCfg(itemId)))) refresh(id);
    }
}
