package com.jjg.game.slots.game.hulk.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryAwardInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.hulk.HulkConstant;
import com.jjg.game.slots.game.hulk.data.HulkAwardLineInfo;
import com.jjg.game.slots.game.hulk.data.HulkResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2026/1/15
 */
@Component
public class HulkGenerateManager extends AbstractSlotsGenerateManager<HulkAwardLineInfo, HulkResultLib> {

    public HulkGenerateManager() {
        super(HulkResultLib.class);
    }

    @Override
    public void changeSampleCallbackCollector() {
        log.warn("绿巨人 无法重载配置表");
    }

    @Override
    protected HulkAwardLineInfo getAwardLineInfo() {
        return new HulkAwardLineInfo();
    }

    @Override
    public void onMergeFreeResults(HulkResultLib lib, int addCount) {
        lib.setAddFreeCount(addCount);
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(HulkResultLib lib) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }
        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(lib.getIconArr());

        log.debug("检查全局分散");

        //有多少列有wild
        int specialWildColumCount = wildColumCount(lib.getIconArr());

        //小游戏
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();

        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();

            //检查出现的个数是否满足
            int elementsCount = 0;

            boolean wildIcon = false;
            for (int iconId : cfg.getElementId()) {
                Integer count = showCountMap.get(iconId);
                if (count != null) {
                    elementsCount += count;
                }

                if (!wildIcon) {
                    wildIcon = (iconId == HulkConstant.BaseElement.SPECIAL_WILD);
                }
            }

            if (wildIcon) {
                if (specialWildColumCount != cfg.getRewardNum()) {
                    continue;
                }
            } else {
                if (elementsCount != cfg.getRewardNum()) {
                    continue;
                }
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
                lib.setTriggerTimes(cfg.getBet());
            }


            //是否有jackpot
            if (cfg.getJackpotID() > 0) {
                lib.addJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;
    }

    /**
     * 检查第三列是否有wild
     *
     * @param arr
     * @return
     */
    private int wildColumCount(int[] arr) {
        if (arr == null || arr.length < 1) {
            return 0;
        }

        int count = 0;
        for (int i = 7; i <= 9; i++) {
            if (arr[i] == HulkConstant.BaseElement.SPECIAL_WILD) {
                count = 1;
                break;
            }
        }

        if (count > 0) {
            for (int i = 4; i <= 6; i++) {
                if (arr[i] == HulkConstant.BaseElement.SPECIAL_WILD) {
                    count++;
                    break;
                }
            }

            for (int i = 10; i <= 12; i++) {
                if (arr[i] == HulkConstant.BaseElement.SPECIAL_WILD) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    @Override
    public void calTimes(HulkResultLib lib) throws Exception {
        //把嵌套的免费结果拍平到最外层 freeGames，并通过 onMergeFreeResults 给触发免费的那条结果赋 addFreeCount
        List<JSONObject> freeGames = new ArrayList<>();
        mergeFreeResults(lib, freeGames, true);

        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //小游戏
        lib.addTimes(miniGame(lib));
        //免费
        lib.addTimes(calFree(lib));
    }

    @Override
    public void mergeFreeResults(HulkResultLib lib, List<JSONObject> freeGames, boolean init) {
        if (lib == null) {
            return;
        }

        if (init && (lib.getLibTypeSet() == null || !lib.getLibTypeSet().contains(HulkConstant.SpecialMode.FREE))) {
            return;
        }

        List<SpecialAuxiliaryInfo> auxiliaryInfos = lib.getSpecialAuxiliaryInfoList();
        if (CollectionUtil.isEmpty(auxiliaryInfos)) {
            return;
        }
        for (int i = auxiliaryInfos.size() - 1; i >= 0; i--) {
            SpecialAuxiliaryInfo auxiliaryInfo = auxiliaryInfos.get(i);
            if (CollectionUtil.isEmpty(auxiliaryInfo.getFreeGames())) {
                continue;
            }

            SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(auxiliaryInfo.getCfgId());
            if (specialAuxiliaryCfg == null || specialAuxiliaryCfg.getType() != HulkConstant.SpecialAuxiliary.FREE_SPIN) {
                continue;
            }

            if (!init) {
                onMergeFreeResults(lib, auxiliaryInfo.getFreeGames().size());
            }
            //将免费结果库添加到最开始的lib中 通用自动addFreeCount
            for (JSONObject freeGame : auxiliaryInfo.getFreeGames()) {
                HulkResultLib freeLib = freeGame.toJavaObject(lib.getClass());
                if (CollectionUtil.isNotEmpty(freeLib.getSpecialAuxiliaryInfoList())) {
                    boolean hasNestedFree = false;
                    for (SpecialAuxiliaryInfo info : freeLib.getSpecialAuxiliaryInfoList()) {
                        if (CollectionUtil.isEmpty(info.getFreeGames())) {
                            continue;
                        }

                        SpecialAuxiliaryCfg tmpSpecialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(info.getCfgId());
                        if (tmpSpecialAuxiliaryCfg == null || tmpSpecialAuxiliaryCfg.getType() != HulkConstant.SpecialAuxiliary.FREE_SPIN) {
                            continue;
                        }
                        hasNestedFree = true;
                        break;
                    }
                    if (hasNestedFree) {
                        List<JSONObject> nestedFreeGames = new ArrayList<>();
                        mergeFreeResults(freeLib, nestedFreeGames, false);
                        freeGames.add((JSONObject) JSON.toJSON(freeLib));
                        freeGames.addAll(nestedFreeGames);
                        continue;
                    }
                }
                freeGames.add((JSONObject) JSON.toJSON(freeLib));
            }
            if (init) {
                auxiliaryInfo.setFreeGames(freeGames);
            } else {
                auxiliaryInfos.remove(i);
            }
        }

    }

    /**
     * 单线倍数
     *
     * @param awardLineInfoList
     * @return
     */
    public long calLineTimes(List<HulkAwardLineInfo> awardLineInfoList) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (HulkAwardLineInfo info : awardLineInfoList) {
            times += info.getBaseTimes();
        }
        return times;
    }

    private long miniGame(HulkResultLib lib) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }

        long times = 0;
        for (SpecialAuxiliaryInfo info : lib.getSpecialAuxiliaryInfoList()) {
            SpecialAuxiliaryCfg cfg = GameDataManager.getSpecialAuxiliaryCfg(info.getCfgId());
            if (cfg.getType() != HulkConstant.SpecialAuxiliary.MINI_GAME) {
                continue;
            }

            if (info.getAwardInfos() == null || info.getAwardInfos().isEmpty()) {
                continue;
            }

            for (SpecialAuxiliaryAwardInfo awardInfo : info.getAwardInfos()) {
                if (awardInfo.getAwardCList() == null || awardInfo.getAwardCList().isEmpty()) {
                    continue;
                }

                int sum = 0;
                for (int i : awardInfo.getAwardCList()) {
                    sum += i;
                }

                times += sum * awardInfo.getAwardD();
            }
        }
        return times;
    }

    @Override
    protected long calFree(HulkResultLib lib) throws Exception {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }

        //修改触发局的倍数
        lib.setTriggerTimes(lib.getTriggerTimes() + calLineTimes(lib.getAwardLineInfoList()));

        long times = lib.getTriggerTimes();

        //触发了免费后，倍数*3
        if (lib.getLibTypeSet() != null && lib.getLibTypeSet().contains(HulkConstant.SpecialMode.FREE)) {
            for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
                if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                    continue;
                }

                //免费局的总倍数，无翻倍
//                long tmpFreeAllTimes = 0;

                List<JSONObject> newFreeGames = new ArrayList<>();
                for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                    HulkResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), this.resultLibClazz);
//                    tmpFreeAllTimes += tmpLib.getTimes();
                    if (tmpLib.getTimes() > 0) {
                        tmpLib.setTimes(tmpLib.getTimes() * 3);
                        newFreeGames.add((JSONObject) JSON.toJSON(tmpLib));
                    } else {
                        newFreeGames.add(jsonObject);
                    }
                    times += tmpLib.getTimes();
                }
                specialAuxiliaryInfo.setFreeGames(newFreeGames);
            }

        } else {
            for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
                if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                    continue;
                }
                for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                    HulkResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), this.resultLibClazz);
                    times += tmpLib.getTimes();
                }
            }
        }
        return times;
    }
}
