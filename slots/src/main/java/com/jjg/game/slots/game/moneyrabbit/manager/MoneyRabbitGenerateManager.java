package com.jjg.game.slots.game.moneyrabbit.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialGirdCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryAwardInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.moneyrabbit.MoneyRabbitConstant;
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
    protected MoneyRabbitAwardLineInfo getAwardLineInfo() {
        return new MoneyRabbitAwardLineInfo();
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

            lib.addJackpotId(cfg.getJackpotID());
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    public void calTimes(MoneyRabbitResultLib lib) throws Exception {
        if (!checkElement(lib)) {
            log.warn("lib = {}", JSONObject.toJSONString(lib));
            throw new IllegalArgumentException("检查结果有错误");
        }

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

    @Override
    protected long calFree(MoneyRabbitResultLib lib) throws Exception {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }

        long times = 0;
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                continue;
            }

            for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                MoneyRabbitResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), this.resultLibClazz);
                //中奖线
                lib.addTimes(calLineTimes(tmpLib.getAwardLineInfoList()));
                times += tmpLib.getTimes();
            }
        }
        return times;
    }

    private boolean checkElement(MoneyRabbitResultLib lib) {
        int coinSize = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            int coin = lib.getIconArr()[i];
            if (coin == MoneyRabbitConstant.BaseElement.ID_COIN) {
                coinSize++;
            }
        }

        if (coinSize < 1) {
            return true;
        }

        if (lib.getSpecialGirdInfoList() == null || lib.getSpecialGirdInfoList().isEmpty()) {
            log.warn("coinSize = {}", coinSize);
            return false;
        }

        for (SpecialGirdInfo specialGirdInfo : lib.getSpecialGirdInfoList()) {
            SpecialGirdCfg specialGirdCfg = GameDataManager.getSpecialGirdCfg(specialGirdInfo.getCfgId());
            if (specialGirdCfg == null) {
                log.warn("获取配置为空 cfgId = {}", specialGirdInfo.getCfgId());
                return false;
            }
            if (specialGirdCfg.getElement() == null || specialGirdCfg.getElement().isEmpty()) {
                continue;
            }
            if (specialGirdCfg.getElement().containsKey(MoneyRabbitConstant.BaseElement.ID_COIN) || specialGirdCfg.getElement().size() == coinSize) {
                return true;
            }
        }
        return false;
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

    /**
     * 特殊模式
     *
     * @param lib
     * @return
     */
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

            SpecialAuxiliaryAwardInfo specialAuxiliaryAwardInfo = specialAuxiliaryInfo.getAwardInfos().stream().findFirst().orElseGet(null);
            if (specialAuxiliaryAwardInfo == null) {
                continue;
            }

            SpecialAuxiliaryCfg cfg = GameDataManager.getSpecialAuxiliaryCfg(specialAuxiliaryInfo.getCfgId());
            if (cfg == null) {
                continue;
            }

            int randCount = specialAuxiliaryAwardInfo.getRandCount();

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
            times *= randCount;
        }
        return times;
    }
}
