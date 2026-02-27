package com.jjg.game.slots.game.luckymouse.manager;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.game.luckymouse.LuckyMouseConstant;
import com.jjg.game.slots.game.luckymouse.data.LuckyMouseAwardLineInfo;
import com.jjg.game.slots.game.luckymouse.data.LuckyMouseResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class LuckyMouseGenerateManager extends AbstractSlotsGenerateManager<LuckyMouseAwardLineInfo, LuckyMouseResultLib> {
    public LuckyMouseGenerateManager() {
        super(LuckyMouseResultLib.class);
    }

    private Pair<Integer, Integer> modelRandom;

    public Pair<Integer, Integer> getModelRandom() {
        return modelRandom;
    }

    @Override
    protected LuckyMouseAwardLineInfo getAwardLineInfo() {
        return new LuckyMouseAwardLineInfo();
    }

    @Override
    protected void specialPlayConfig() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(LuckyMouseConstant.SpecialPlay.FU_SHU_TRIGGER_ID);
        if (specialPlayCfg == null  || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] modeArr = specialPlayCfg.getValue().split(",");
        if (modeArr.length != 2) {
            return;
        }
        modelRandom = Pair.newPair(Integer.parseInt(modeArr[0]), Integer.parseInt(modeArr[1]));
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(LuckyMouseResultLib lib) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }

        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(lib.getIconArr());
        log.debug("检查全局分散");

        //小游戏
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();

        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            //检查出现的个数是否满足
            int elementsCount = 0;
            for (int iconId : cfg.getElementId()) {
                Integer count = showCountMap.get(iconId);
                if (count != null) {
                    elementsCount += count;
                }
            }
            if (elementsCount != cfg.getRewardNum()) {
                continue;
            }
            //是否触发小游戏
            if (cfg.getFeatureTriggerId() != null && !cfg.getFeatureTriggerId().isEmpty()) {
                if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
                    Set<Integer> triggerFreeSet = SlotsConst.specialModeTriggerFreeModeIds.get(this.gameType);
                    if (triggerFreeSet != null && !triggerFreeSet.isEmpty()) {
                        cfg.getFeatureTriggerId().forEach(miniGameId -> {
                            triggerFreeSet.forEach(libType -> {
                                SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                                if (specialAuxiliaryInfo != null) {
                                    specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                                }
                            });

                        });
                    }
                } else {
                    cfg.getFeatureTriggerId().forEach(miniGameId -> {
                        lib.getLibTypeSet().forEach(libType -> {
                            SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                            if (specialAuxiliaryInfo != null) {
                                specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                            }
                        });

                    });
                }
            }

            if (cfg.getJackpotID() > 0) {
                lib.setJackpotId(cfg.getJackpotID());
                break;
            }
        }
        return specialAuxiliaryInfoList;
    }


    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }
        int maxTryCount = 9999;
        boolean hit = false;
        for (int i = 0; i < maxTryCount; i++) {
            LuckyMouseResultLib resultLib = generateFreeOne(specialModeType, specialAuxiliaryCfg, 0);
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(resultLib));
            // 中奖后直接跳出免费模式
            if (CollUtil.isNotEmpty(resultLib.getAwardLineInfoList())) {
                hit = true;
                break;
            }
            if (CollUtil.isNotEmpty(specialAuxiliaryInfo.getFreeGames()) && specialAuxiliaryInfo.getFreeGames().size() >= freeCount) {
                specialAuxiliaryInfo.getFreeGames().clear();
            }
        }
        if (!hit) {
            // 执行了9999次依然没有触发获奖，则需要让策划更改配置，免费模式获奖概率过低
            log.warn("福鼠模式生成结果集已运行9999次还得到获奖结果，需修改配置！");
        }
    }

    @Override
    public void calTimes(LuckyMouseResultLib lib) throws Exception {
        if(triggerFreeLib(lib, LuckyMouseConstant.SpecialMode.FREE)) {
            //免费
            lib.addTimes(calFree(lib));
        } else {
            //中奖线
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        }
    }

    /**
     * 鼠鼠福福的福鼠（免费）模式，只有最后一次会中奖
     */
    @Override
    protected long calFree(LuckyMouseResultLib lib) {
        if (CollUtil.isEmpty(lib.getSpecialAuxiliaryInfoList())) {
            return 0;
        }
        long times = 0;
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (CollUtil.isEmpty(specialAuxiliaryInfo.getFreeGames())) {
                continue;
            }
            JSONObject last = specialAuxiliaryInfo.getFreeGames().getLast();
            LuckyMouseResultLib tmpLib = JSON.parseObject(last.toJSONString(), this.resultLibClazz);
            calLineTimes(tmpLib.getAwardLineInfoList());
            times += tmpLib.getTimes();
        }
        return times;
    }

    private int calLineTimes(List<LuckyMouseAwardLineInfo> list) {
        return CollUtil.isEmpty(list)
                ? 0
                : list.stream()
                .mapToInt(LuckyMouseAwardLineInfo::getBaseTimes)
                .sum();
    }
}
