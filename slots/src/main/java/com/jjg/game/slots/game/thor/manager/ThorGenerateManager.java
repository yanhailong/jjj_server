package com.jjg.game.slots.game.thor.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.thor.ThorConstant;
import com.jjg.game.slots.game.thor.data.ThorAwardLineInfo;
import com.jjg.game.slots.game.thor.data.ThorResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
@Component
public class ThorGenerateManager extends AbstractSlotsGenerateManager<ThorAwardLineInfo, ThorResultLib> {
    public ThorGenerateManager() {
        super(ThorResultLib.class);
    }

    @Override
    protected ThorAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        ThorAwardLineInfo awardLineInfo = new ThorAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);

        for (List<Integer> otherIconList : rewardCfg.getBetTimes()) {
            int iconId = otherIconList.get(0);
            //该元素在这条线上出现的次数
            long showCount = lineList.stream().filter(tmpId -> arr[tmpId] == iconId).count();
            if (showCount == otherIconList.get(1)) {
                int addTimes = otherIconList.get(2);
                awardLineInfo.addSpecialAwardInfo(iconId, addTimes);
//                            log.debug("特殊图标添加倍率 iconId = {},showCount = {},addTimes = {}", iconId, showCount, addTimes);
            }
        }
        return awardLineInfo;
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(ThorResultLib lib) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }

        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(lib.getIconArr());
        //已经出现的小游戏id
        Set<Integer> showAuxiliaryIdSet = new HashSet<>();
        addShowAuxiliaryId(lib, showAuxiliaryIdSet);

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
                cfg.getFeatureTriggerId().forEach(miniGameId -> {
                    if (!showAuxiliaryIdSet.contains(miniGameId)) { //如果没出现过的小游戏可以触发
                        lib.getLibTypeSet().forEach(libType -> {
                            SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
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
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        //最后一局
        int lastOne = freeCount - 1;
        //上一局的冰冻wild
        Set<Integer> lastFreezeWildSet = null;

        for (int i = 0; i < freeCount; i++) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            ThorResultLib lib;
            if (specialModeType == ThorConstant.SpecialMode.FIRE) {  //火焰模式
                if (i == lastOne) {
                    lib = generateLastFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID, null);
                } else {
                    lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
                }
            } else {  //冰雪模式
                //本局的冰冻wild
                Set<Integer> thisFreezeWildSet = new HashSet<>();
                if (i == lastOne) {
                    //如果是最后一局，检查wild有没有出现在7，8位置上
                    if (lastFreezeWildSet != null && (lastFreezeWildSet.contains(7) || lastFreezeWildSet.contains(8))) {
                        i--;
                        lib = generateIceFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID, lastFreezeWildSet, thisFreezeWildSet);
                    } else {
                        lib = generateLastFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID, lastFreezeWildSet);
                        lib.setFreezeWildSet(thisFreezeWildSet);
                    }
                } else {
                    lib = generateIceFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID, lastFreezeWildSet, thisFreezeWildSet);
                    lib.setFreezeWildSet(thisFreezeWildSet);
                }
                lastFreezeWildSet = new HashSet<>(thisFreezeWildSet);

            }

            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
        }
    }

    public ThorResultLib generateIceFreeOne(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID, Set<Integer> lastFreezeWildSet, Set<Integer> thisFreezeWildSet) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }

            //创建结果库对象
            ThorResultLib lib = createResultLib();
            lib.setId(RandomUtils.getUUid());
            lib.setRollerMode(specialModeCfg.getRollerMode());

            //获取rollerMode
            int rollerMode = specialAuxiliaryCfg.getRollerMode();
            if (rollerMode < 1) {
                rollerMode = specialModeCfg.getRollerMode();
            }

            int[] arr = generateAllIcons(specialModeCfg.getRollerMode(), specialModeCfg.getCols(), specialModeCfg.getRows());
            //生成所有的图标
            if (arr == null) {
                return null;
            }

            log.debug("生成免费游戏图标 arr = {}", Arrays.toString(arr));

            //修改格子策略组
            if (specialGroupGirdID > 0) {
                SpecialGirdInfo specialGirdInfo = gridUpdate(lib, specialGroupGirdID, arr);
                if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                    lib.addSpecialGirdInfo(specialGirdInfo);
                }
            }

            //修改格子
            if (specialAuxiliaryCfg.getSpecialGirdID() != null && !specialAuxiliaryCfg.getSpecialGirdID().isEmpty()) {
                for (int specialGirdCfgId : specialAuxiliaryCfg.getSpecialGirdID()) {
                    SpecialGirdInfo specialGirdInfo = gridUpdate(lib, specialGirdCfgId, arr);
                    if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                        lib.addSpecialGirdInfo(specialGirdInfo);
                    }
                }
            }

            //添加本局的wild
            addFreezeWild(arr, thisFreezeWildSet);

            //将上一局出现的wild冻结到本局
            if (lastFreezeWildSet != null && !lastFreezeWildSet.isEmpty()) {
                for (int wildIndex : lastFreezeWildSet) {
                    //本局中出现的wild和上一局出现wild的位置重叠，那么将改位置上的wild视为上局冻结后的wild
                    thisFreezeWildSet.remove(wildIndex);
                    arr[wildIndex] = ThorConstant.BaseElement.ID_FREEZE_WILD;
                }
            }

            //判断中奖，返回
            return checkAward(arr, lib, true);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * 生成免费模式的最后一局结果
     *
     * @param specialModeType
     * @param specialAuxiliaryCfg
     * @param specialGroupGirdID
     * @return
     */
    private ThorResultLib generateLastFreeOne(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID, Set<Integer> wildSet) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }

            //创建结果库对象
            ThorResultLib lib = createResultLib();
            lib.setId(RandomUtils.getUUid());
            lib.setRollerMode(specialModeCfg.getRollerMode());

            //获取rollerMode
            int rollerMode = specialAuxiliaryCfg.getRollerMode();
            if (rollerMode < 1) {
                rollerMode = specialModeCfg.getRollerMode();
            }

            //生成所有的图标
            int[] arr = generateAllIcons(specialModeCfg.getRollerMode(), specialModeCfg.getCols(), specialModeCfg.getRows());
            if (arr == null) {
                return null;
            }

            log.debug("生成免费游戏图标 arr = {}", Arrays.toString(arr));

            //修改格子策略组
            if (specialGroupGirdID > 0) {
                SpecialGirdInfo specialGirdInfo = gridUpdate(lib, specialGroupGirdID, arr);
                if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                    lib.addSpecialGirdInfo(specialGirdInfo);
                }
            }

            //修改格子
            if (specialAuxiliaryCfg.getSpecialGirdID() != null && !specialAuxiliaryCfg.getSpecialGirdID().isEmpty()) {
                for (int specialGirdCfgId : specialAuxiliaryCfg.getSpecialGirdID()) {
                    SpecialGirdInfo specialGirdInfo = gridUpdate(lib, specialGirdCfgId, arr);
                    if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                        lib.addSpecialGirdInfo(specialGirdInfo);
                    }
                }
            }

            //修改格子
            SpecialGirdInfo specialGirdInfo = gridUpdate(lib, ThorConstant.Common.FREE_LAST_ONE_UPDATE_GIRD, arr);
            if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                lib.addSpecialGirdInfo(specialGirdInfo);
            }

            if (wildSet != null && !wildSet.isEmpty()) {
                for (int wildIndex : wildSet) {
                    arr[wildIndex] = ThorConstant.BaseElement.ID_FREEZE_WILD;
                }
//                lib.setFreezeIndexs(wildSet);
            }

            //判断中奖，返回
            return checkAward(arr, lib, true);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * 获取所有的wild坐标
     *
     * @param arr
     * @return
     */
    private Set<Integer> addFreezeWild(int[] arr, Set<Integer> thisFreezeWildSet) {
        for (int i = 0; i < arr.length; i++) {
            int icon = arr[i];
            if (icon == ThorConstant.BaseElement.ID_FREEZE_WILD) {
                thisFreezeWildSet.add(i);
            }
        }
        return thisFreezeWildSet;
    }

    private boolean hasFireWild(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            int icon = arr[i];
            if (icon == ThorConstant.BaseElement.ID_FIRE_WILD) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void calTimes(ThorResultLib lib) throws Exception {
//        if (!checkElement(lib)) {
//            throw new IllegalArgumentException("检查结果有错误 lib = " + JSONObject.toJSONString(lib));
//        }

        if (triggerFreeLib(lib)) {
            //免费
            lib.addTimes(calFree(lib));
        } else {
            //中奖线
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        }
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    private int calLineTimes(List<ThorAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (ThorAwardLineInfo awardLineInfo : list) {
            if (awardLineInfo.getOtherIconAwardInfoMap() == null || awardLineInfo.getOtherIconAwardInfoMap().isEmpty()) {
                times += awardLineInfo.getBaseTimes();
                continue;
            }
            for (Map.Entry<Integer, Integer> en : awardLineInfo.getOtherIconAwardInfoMap().entrySet()) {
                int tmpTimes = awardLineInfo.getBaseTimes() * en.getValue();
                times += tmpTimes;
            }
        }
        return times;
    }

    /**
     * 计算免费游戏的总倍数
     *
     * @param lib
     * @return
     */
    protected long calFree(ThorResultLib lib) throws Exception {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }

        long times = 0;
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                continue;
            }

            for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                ThorResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), ThorResultLib.class);
                calTimes(tmpLib);
                if (lib.getLibTypeSet().contains(ThorConstant.SpecialMode.ICE)) {
                    if (hasFireWild(tmpLib.getIconArr())) {
                        tmpLib.setTimes(tmpLib.getTimes() * 3);
                    }
                }
                times += tmpLib.getTimes();

            }
        }
        return times;
    }


    /**
     * 检查元素与小游戏所需要的参数是否匹配
     *
     * @param lib
     */
    private boolean checkElement(ThorResultLib lib) {
        if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            return true;
        }

        //检查二选一
        if (lib.getLibTypeSet().contains(ThorConstant.SpecialMode.FREE)
                && !checkTriggerFree(lib)) {
            log.warn("检查免费触发局失败");
            return false;
        }

        //检查jackpool模式
        if (lib.getLibTypeSet().contains(ThorConstant.SpecialMode.JACKPOOL) && !checkJackpool(lib)) {
            log.warn("检查jackpool模式失败");
            return false;
        }

        //检查免费模式
        if ((lib.getLibTypeSet().contains(ThorConstant.SpecialMode.FIRE) || lib.getLibTypeSet().contains(ThorConstant.SpecialMode.ICE)) && !checkFreeModel(lib)) {
            log.warn("检查免费模式失败");
            return false;
        }
        return true;
    }

    /**
     * 检查免费触发局
     *
     * @param lib
     * @return
     */
    private boolean checkTriggerFree(ThorResultLib lib) {
        int count = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == ThorConstant.BaseElement.ID_SCATTER) {
                count++;
            }
        }
        return count >= 3;
    }

    /**
     * 检查奖池模式
     *
     * @param lib
     * @return
     */
    private boolean checkJackpool(ThorResultLib lib) {
        if (lib.jackpotEmpty()) {
            return false;
        }

        int count = 0;
        int jackpool = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == ThorConstant.BaseElement.ID_SCATTER) {
                count++;
            } else if (icon == ThorConstant.BaseElement.ID_MINI || icon == ThorConstant.BaseElement.ID_MINOR ||
                    icon == ThorConstant.BaseElement.ID_MAJOR || icon == ThorConstant.BaseElement.ID_GRAND) {
                jackpool++;
            }
        }
        return count >= 2 && jackpool > 0;
    }

    private boolean checkFreeModel(ThorResultLib lib) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return false;
        }

        SpecialGirdCfg cfg = GameDataManager.getSpecialGirdCfg(ThorConstant.Common.FREE_LAST_ONE_UPDATE_GIRD);
        int specialIcon = cfg.getElement().entrySet().stream().findFirst().get().getKey();

        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                continue;
            }

            int len = specialAuxiliaryInfo.getFreeGames().size();
            int lastIndex = len - 1;
            for (int i = 0; i < len; i++) {
                JSONObject jsonObject = specialAuxiliaryInfo.getFreeGames().get(i);
                ThorResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), ThorResultLib.class);

                int icon7 = tmpLib.getIconArr()[7];
                int icon8 = tmpLib.getIconArr()[8];

                boolean iconLast = (icon7 == specialIcon && icon8 == specialIcon);
                if (i == lastIndex) {
                    if (!iconLast) {
                        log.debug("免费最后一局没有特殊图标 specialIcon = {}", specialIcon);
                        return false;
                    }
                } else {
                    if (iconLast) {
                        log.debug("免费中途不能有特殊图标 specialIcon = {}", specialIcon);
                        return false;
                    }
                }
            }

        }
        return true;
    }

    /**
     * 添加已经出现的小游戏id
     *
     * @param lib
     * @param set
     */
    private void addShowAuxiliaryId(ThorResultLib lib, Set<Integer> set) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return;
        }

        lib.getSpecialAuxiliaryInfoList().forEach(info -> {
            set.add(info.getCfgId());
        });
    }
}
