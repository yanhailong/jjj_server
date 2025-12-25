package com.jjg.game.slots.game.moneyrabbit.manager;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitAwardLineInfo;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import com.jjg.game.slots.utils.SlotsUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MoneyRabbitGenerateManager extends AbstractSlotsGenerateManager<MoneyRabbitAwardLineInfo, MoneyRabbitResultLib> {
    public MoneyRabbitGenerateManager() {
        super(MoneyRabbitResultLib.class);
    }

    @Override
    protected MoneyRabbitAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        MoneyRabbitAwardLineInfo awardLineInfo = new MoneyRabbitAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);

        return awardLineInfo;
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(MoneyRabbitResultLib lib) {
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

            lib.setJackpotId(cfg.getJackpotID());
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    public void calTimes(MoneyRabbitResultLib lib) throws Exception {
        if (triggerFreeLib(lib)) {
            //免费
            lib.addTimes(calFree(lib));
        } else {
            //中奖线
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
            //金钱兔模式
            lib.addTimes(calSpecialModel(lib));
        }
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    private int calLineTimes(List<MoneyRabbitAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (MoneyRabbitAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    private int calSpecialModel(MoneyRabbitResultLib lib) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }
        if (lib.getSpecialGirdInfoList() == null || lib.getSpecialGirdInfoList().isEmpty()) {
            return 0;
        }

        int times = 0;
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getAwardInfos() == null || specialAuxiliaryInfo.getAwardInfos().isEmpty()) {
                continue;
            }
            SpecialAuxiliaryCfg cfg = GameDataManager.getSpecialAuxiliaryCfg(specialAuxiliaryInfo.getCfgId());
            int prop = cfg.getAwardTypeA().get(1);
            if (!SlotsUtil.calProp(prop)) {
                continue;
            }

            int icon = cfg.getAwardTypeA().get(0);

            for (SpecialGirdInfo specialGirdInfo : lib.getSpecialGirdInfoList()) {
                if (specialGirdInfo.getValueMap() == null || specialGirdInfo.getValueMap().isEmpty()) {
                    continue;
                }

                for (Map.Entry<Integer, Integer> en : specialGirdInfo.getValueMap().entrySet()) {
                    if (lib.getIconArr()[en.getKey()] == icon) {
                        times += en.getValue();
                    }
                }
            }
        }
        return times;
    }
}
