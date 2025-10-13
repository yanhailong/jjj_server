package com.jjg.game.activity.util;

import cn.hutool.core.lang.WeightRandom;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/9/22 19:34
 */
@Component
public class DataCache implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(DataCache.class);
    //32 官方派奖：有效下注转换积分比例 ：有效下注 = X积分
    private Pair<Integer, Integer> effectiveWaterFlowConvertRatio;
    //33 官方派奖：充值转换积分比例 ：充值金额 = X积分
    private Pair<Integer, Integer> rechargeConvertRatio;
    //官方派奖机器人中奖配置 毫秒数->权重
    private WeightRandom<Integer> robotRandom;

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::initCacheData)
                .addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::initCacheData);
    }

    public void initCacheData() {
        GlobalConfigCfg globalConfigCfg;
        try {
            globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.OfficialAwards.EFFECTIVE_WATER_FLOW_CONVERT_RATIO);
            effectiveWaterFlowConvertRatio = getIntIntPair(globalConfigCfg);
        } catch (Exception e) {
            log.error("init cacheData error!", e);
        }
        try {
            globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.OfficialAwards.RECHARGE_CONVERT_RATIO);
            rechargeConvertRatio = getIntIntPair(globalConfigCfg);
        } catch (Exception e) {
            log.error("init cacheData error!", e);

        }

        globalConfigCfg = GameDataManager.getGlobalConfigCfg(ActivityConstant.OfficialAwards.ROBOT_CFG);
        if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
            String[] split = StringUtils.split(globalConfigCfg.getValue(), "|");
            WeightRandom<Integer> random = new WeightRandom<>();
            for (String config : split) {
                String[] detailConfig = StringUtils.split(config, "_");
                if (detailConfig.length == 2) {
                    random.add(Integer.parseInt(detailConfig[0]), Integer.parseInt(detailConfig[1]));
                }
            }
            if (random.next() != null) {
                robotRandom = random;
            }
        }

    }

    public Pair<Integer, Integer> getIntIntPair(GlobalConfigCfg globalConfigCfg) {
        if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
            String[] split = StringUtils.split(globalConfigCfg.getValue(), "_");
            if (split.length == 2) {
                return new Pair<>(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
            }
        }
        return null;
    }
    public Pair<Integer, Integer> getEffectiveWaterFlowConvertRatio() {
        return effectiveWaterFlowConvertRatio;
    }

    public Pair<Integer, Integer> getRechargeConvertRatio() {
        return rechargeConvertRatio;
    }

    public WeightRandom<Integer> getRobotRandom() {
        return robotRandom;
    }
}
