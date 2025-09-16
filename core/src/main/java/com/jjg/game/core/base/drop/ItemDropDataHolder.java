package com.jjg.game.core.base.drop;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.DropGroupCfg;
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

    // 分组ID <=> 掉落分组配置
    private final Map<Integer, List<DropGroupCfg>> dropGroupCfgMap = new HashMap<>();

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(DropGroupCfg.EXCEL_NAME, this::reloadGameCacheData)
            .addChangeSampleFileObserveWithCallBack(DropGroupCfg.EXCEL_NAME, this::reloadGameCacheData);
    }

    /**
     * 重载游戏缓存数据
     */
    private void reloadGameCacheData() {
        dropGroupCfgMap.clear();
        for (DropGroupCfg dropGroupCfg : GameDataManager.getDropGroupCfgList()) {
            // 缓存
            dropGroupCfgMap.computeIfAbsent(dropGroupCfg.getTrunkID(), k -> new ArrayList<>()).add(dropGroupCfg);
        }
    }

    /**
     * 获取掉落分组配置数据
     */
    public List<DropGroupCfg> getDropGroupCfg(int trunkId) {
        return dropGroupCfgMap.getOrDefault(trunkId, new ArrayList<>());
    }
}
