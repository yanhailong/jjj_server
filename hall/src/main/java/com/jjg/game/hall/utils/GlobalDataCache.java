package com.jjg.game.hall.utils;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.PokerPoolCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 全局表配置缓存
 *
 * @author lm
 * @date 2025/8/19 09:47
 */
@Component
public class GlobalDataCache implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(GlobalDataCache.class);
    //道具信息 持续时间
    private static Pair<Item, Integer> buyClaimAllRewardsConsumer;
    //道具信息 减少时间
    private static Pair<Item, Integer> reduceTimeConfig;

    @Override
    public void initSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, GlobalDataCache::changeCacheData)
                .addInitSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, GlobalDataCache::changeCacheData);
    }

    public static void changeCacheData() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.BUY_ALL_CLAIM_ALL_REWARDS);
        if (Objects.nonNull(globalConfigCfg)) {
            String cfgValue = globalConfigCfg.getValue();
            if (StringUtils.isNotEmpty(cfgValue)) {
                try {
                    String[] cfgValueArr = StringUtils.split(cfgValue, "_");
                    if (cfgValueArr.length == 3) {
                        Item item = new Item(Integer.parseInt(cfgValueArr[0]), Integer.parseInt(cfgValueArr[1]));
                        buyClaimAllRewardsConsumer = Pair.newPair(item, Integer.parseInt(cfgValueArr[2]));
                    }
                } catch (Exception e) {
                    log.error("解析购买一键领取消耗道具失败,e");
                }
            }
        }
        globalConfigCfg = GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.CASINO_REDUCE_TIME_CONFIG);
        if (Objects.nonNull(globalConfigCfg)) {
            String cfgValue = globalConfigCfg.getValue();
            if (StringUtils.isNotEmpty(cfgValue)) {
                try {
                    String[] cfgValueArr = StringUtils.split(cfgValue, "_");
                    if (cfgValueArr.length == 3) {
                        Item item = new Item(Integer.parseInt(cfgValueArr[0]), Integer.parseInt(cfgValueArr[1]));
                        reduceTimeConfig = Pair.newPair(item, Integer.parseInt(cfgValueArr[2]));
                    }
                } catch (Exception e) {
                    log.error("解析reduceTimeConfig失败,e");
                }
            }
        }
    }

    public static Pair<Item, Integer> getReduceTimeConfig() {
        return reduceTimeConfig;
    }

    public static Pair<Item, Integer> getBuyClaimAllRewardsConsumer() {
        return buyClaimAllRewardsConsumer;
    }
}
