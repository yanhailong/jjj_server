package com.jjg.game.slots.game.panJinLian.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.game.panJinLian.PanJinLianConstant;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianAddFreeInfo;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianAwardLineInfo;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 潘金莲结果生成管理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:33
 */
@Component
public class PanJinLianGenerateManager extends AbstractSlotsGenerateManager<PanJinLianAwardLineInfo, PanJinLianResultLib> {

    public PanJinLianGenerateManager() {
        super(PanJinLianResultLib.class);
    }

    private PanJinLianAddFreeInfo panJinLianAddFreeInfo;

    @Override
    protected PanJinLianAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        PanJinLianAwardLineInfo awardLineInfo = new PanJinLianAwardLineInfo();
        Set<Integer> icons = new HashSet<>(baseLineCfg.getPosLocation());
        awardLineInfo.setSameIconSet(icons);
        awardLineInfo.setSameIcon(rewardCfg.getElementId().getFirst() % 10);
        awardLineInfo.setLineId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        return awardLineInfo;
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(PanJinLianResultLib lib) {
        // 获取全局分散奖励配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap =
                this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }

        // 统计图标出现次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(lib.getIconArr());
        Set<Integer> showAuxiliaryIdSet = new HashSet<>();
        addShowAuxiliaryId(lib, showAuxiliaryIdSet);

        log.debug("检查全局分散触发");
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();

        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();

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

            if (cfg.getFeatureTriggerId() != null && !cfg.getFeatureTriggerId().isEmpty()) {
                int count = checkAddFreeCount(lib);
                if (count > 0) {
                    Set<Integer> libTypeSet = new HashSet<>();
                    libTypeSet.add(PanJinLianConstant.SpecialMode.FREE);
                    lib.setLibTypeSet(libTypeSet);
                }
                cfg.getFeatureTriggerId().forEach(miniGameId -> {
                    if (!showAuxiliaryIdSet.contains(miniGameId)) {
                        lib.getLibTypeSet().forEach(libType -> {
                            SpecialAuxiliaryInfo specialAuxiliaryInfo =
                                    triggerMiniGame(libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                            if (specialAuxiliaryInfo != null) {
                                showAuxiliaryIdSet.add(miniGameId);
                                specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                            }
                        });
                    }
                });
            }

            if (lib.getJackpotIds() == null || lib.getJackpotIds().isEmpty()) {
                lib.addJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg,
                               SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        log.debug("增加免费次数 addCount={}", freeCount);
        int remainFreeCount = freeCount;
        while (remainFreeCount > 0) {
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            PanJinLianResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            int addCount = checkAddFreeCount(lib);
            log.debug("免费结果中追加次数={}", addCount);
            lib.setAddFreeCount(addCount);
            remainFreeCount += addCount;
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
            remainFreeCount--;
        }
    }

    /**
     * 检查是否增加免费次数
     */
    private int checkAddFreeCount(PanJinLianResultLib lib) {
        if (panJinLianAddFreeInfo == null || panJinLianAddFreeInfo.getLibType() != PanJinLianConstant.SpecialMode.FREE) {
            return 0;
        }
        int times = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon != panJinLianAddFreeInfo.getTargetIcon()) {
                continue;
            }
            times++;
        }
        return panJinLianAddFreeInfo.getAddFreeCount(times);
    }

    @Override
    public void calTimes(PanJinLianResultLib lib) throws Exception {
        if (!checkElement(lib)) {
            throw new IllegalArgumentException("结果库校验失败 lib=" + JSONObject.toJSONString(lib));
        }

        if (triggerFreeLib(lib, PanJinLianConstant.SpecialMode.FREE)) {
            lib.addTimes(calFree(lib));
        } else {
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        }
    }

    /**
     * 计算中奖线倍数
     */
    public int calLineTimes(List<PanJinLianAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int times = 0;
        for (PanJinLianAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    private void addShowAuxiliaryId(PanJinLianResultLib lib, Set<Integer> set) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return;
        }
        lib.getSpecialAuxiliaryInfoList().forEach(info -> set.add(info.getCfgId()));
    }

    /**
     * 检查奖池模式触发条件
     */
    private boolean checkJackpool(PanJinLianResultLib lib) {
        if (lib.jackpotEmpty()) {
            return false;
        }
        int count = 0;
        int jackpool = 0;
        for (int icon : lib.getIconArr()) {
            if (icon == PanJinLianConstant.BaseElement.ID_SCATTER) {
                count++;
            } else if (icon == PanJinLianConstant.BaseElement.ID_MINI
                    || icon == PanJinLianConstant.BaseElement.ID_MINOR
                    || icon == PanJinLianConstant.BaseElement.ID_MAJOR
                    || icon == PanJinLianConstant.BaseElement.ID_GRAND) {
                jackpool++;
            }
        }
        return count >= 2 && jackpool > 0;
    }

    /**
     * 检查免费触发条件
     */
    private boolean checkTriggerFree(PanJinLianResultLib lib) {
        int count = 0;
        for (int icon : lib.getIconArr()) {
            if (icon == PanJinLianConstant.BaseElement.ID_SCATTER) {
                count++;
            }
        }
        return count >= 3;
    }

    /**
     * 校验结果库与玩法模式是否匹配
     */
    private boolean checkElement(PanJinLianResultLib lib) {
        if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            return true;
        }

        if (lib.getLibTypeSet().contains(PanJinLianConstant.SpecialMode.FREE) && !checkTriggerFree(lib)) {
            log.warn("免费模式触发校验失败");
            return false;
        }

        if (lib.getLibTypeSet().contains(PanJinLianConstant.SpecialMode.JACKPOOL) && !checkJackpool(lib)) {
            log.warn("奖池模式触发校验失败");
            return false;
        }
        return true;
    }

    @Override
    protected void specialPlayConfig() {
        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            // 增加免费次数配置
            if (cfg.getPlayType() == PanJinLianConstant.SpecialPlay.TYPE_ADD_FREE_COUNT) {
                PanJinLianAddFreeInfo tmpInfo = new PanJinLianAddFreeInfo();
                String[] arr0 = cfg.getValue().split(",");
                tmpInfo.setLibType(Integer.parseInt(arr0[0]));
                tmpInfo.setTargetIcon(Integer.parseInt(arr0[1]));

                String[] arr1 = arr0[2].split("\\|");
                for (String addFreeInfoStr : arr1) {
                    String[] arr2 = addFreeInfoStr.split("_");
                    int times = Integer.parseInt(arr2[0]);
                    int addFreeCount = Integer.parseInt(arr2[1]);
                    int prop = Integer.parseInt(arr2[2]);
                    tmpInfo.addTimesInfo(times, addFreeCount, prop);
                }
                this.panJinLianAddFreeInfo = tmpInfo;
            }
        }
    }
}
