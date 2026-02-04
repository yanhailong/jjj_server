package com.jjg.game.slots.game.frozenThrone.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.config.ConfigManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.game.frozenThrone.FrozenThroneConstant;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneAddFreeInfo;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneAwardLineInfo;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneResultLib;
import com.jjg.game.slots.game.superstar.data.SuperStarAwardLineInfo;
import com.jjg.game.slots.game.thor.data.ThorAwardLineInfo;
import com.jjg.game.slots.game.wealthgod.data.WealthGodAwardLineInfo;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import com.jjg.game.slots.utils.SlotsUtil;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @auFrozenThrone lihaocao
 * @date 2025/12/2 17:33
 */
@Component
public class FrozenThroneGenerateManager extends AbstractSlotsGenerateManager<FrozenThroneAwardLineInfo, FrozenThroneResultLib> {

    public FrozenThroneGenerateManager() {
        super(FrozenThroneResultLib.class);
    }

    private FrozenThroneAddFreeInfo frozenThroneAddFreeInfo;

    @Override
    protected FrozenThroneAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        FrozenThroneAwardLineInfo awardLineInfo = new FrozenThroneAwardLineInfo();
        Set<Integer> icons = new HashSet<>();
        icons.addAll(baseLineCfg.getPosLocation());
        awardLineInfo.setSameIconSet(icons);
        awardLineInfo.setSameIcon(rewardCfg.getElementId().getFirst() % 10);
        awardLineInfo.setLineId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        return awardLineInfo;
    }


    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(FrozenThroneResultLib lib) {
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
                int count = checkAddFreeCount(lib);
                if (count > 0) {
                    Set<Integer> libTypeSet = new HashSet<>();
                    libTypeSet.add(FrozenThroneConstant.SpecialMode.FREE);
                    lib.setLibTypeSet(libTypeSet);
                }
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

            if(lib.getJackpotIds() == null || lib.getJackpotIds().isEmpty()){
                lib.addJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;
    }

//    @Override
//    public FrozenThroneResultLib checkAward(int[] arr, FrozenThroneResultLib lib, boolean freeModel) throws Exception {
//        if (freeModel) {
//            lib.setGameType(this.gameType);
//            lib.setIconArr(arr);
//            //检查满线图案
//            List<FrozenThroneAwardLineInfo> fullLineInfoList = fullLine(arr);
//            lib.addAllAwardLineInfo(fullLineInfoList);
//
//            //新增 检查并创建玩法
//
//
//            //检查全局分散图案
//            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
//            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);
//
//            calTimes(lib);
//            return lib;
//        } else {
//            lib.setGameType(this.gameType);
//            lib.setIconArr(arr);
//
//            //检查满线图案
//            List<FrozenThroneAwardLineInfo> fullLineInfoList = fullLine(lib);
//            lib.addAllAwardLineInfo(fullLineInfoList);
//
//            //检查全局分散图案
//            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
//            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);
//
//            calTimes(lib);
//
//            return lib;
//        }
//    }

//    @Override
//    protected FrozenThroneAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg) {
//        FrozenThroneAwardLineInfo info = new FrozenThroneAwardLineInfo();
//
//        info.setSameIconSet(sameIconIndexSet);
//        info.setSameIcon(cfg.getElementId().getFirst() % 10);
//
//        if (info.getSameIconSet() != null && !info.getSameIconSet().isEmpty()) {
//            //记录每一列中奖的个数
//            BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
//
//            Map<Integer, Integer> columIconCountMap = new HashMap<>();
//            for (int index : info.getSameIconSet()) {
//                //根据坐标，计算它在哪一列
//                int colId = index / baseInitCfg.getRows();
//                if ((index % baseInitCfg.getRows()) != 0) {
//                    colId++;
//                }
//                columIconCountMap.merge(colId, 1, Integer::sum);
//            }
//
//            int addTimes = 1;
//            for (Map.Entry<Integer, Integer> en : columIconCountMap.entrySet()) {
//                addTimes *= en.getValue();
//            }
//
//            info.setBaseTimes(cfg.getBet() * addTimes);
//        } else {
//            info.setBaseTimes(cfg.getBet());
//        }
//        return info;
//    }

    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg,
                               SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        log.debug("增加免费游戏次数 addCount = {}", freeCount);

        int remainFreeCount = freeCount;

        while (remainFreeCount > 0) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            FrozenThroneResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            int addCount = checkAddFreeCount(lib);
            log.debug("免费转新加 {}",addCount);
            lib.setAddFreeCount(addCount);
            remainFreeCount += addCount;
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
            log.debug("--------------{}------------", remainFreeCount);
            remainFreeCount--;
        }
    }

    /**
     * 检查是否增加免费次数
     *
     * @param lib
     * @return
     */
    private int checkAddFreeCount(FrozenThroneResultLib lib) {

        if (frozenThroneAddFreeInfo.getLibType() != FrozenThroneConstant.SpecialMode.FREE) {
            return 0;
        }
        int times = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            //是否出现了目标图标
            if (icon != frozenThroneAddFreeInfo.getTargetIcon()) {
                continue;
            }
            times++;
        }

        int addFreeCount = frozenThroneAddFreeInfo.getAddFreeCount(times);
        return addFreeCount;
    }


    @Override
    public void calTimes(FrozenThroneResultLib lib) throws Exception {
//        if (!checkElement(lib)) {
//            throw new IllegalArgumentException("检查结果有错误 lib = " + JSONObject.toJSONString(lib));
//        }

        if(triggerFreeLib(lib,FrozenThroneConstant.SpecialMode.FREE)){
            //免费
            lib.addTimes(calFree(lib));
        }else {
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
    public int calLineTimes(List<FrozenThroneAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (FrozenThroneAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    /**
     * 添加已经出现的小游戏id
     *
     * @param lib
     * @param set
     */
    private void addShowAuxiliaryId(FrozenThroneResultLib lib, Set<Integer> set) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return;
        }

        lib.getSpecialAuxiliaryInfoList().forEach(info -> {
            set.add(info.getCfgId());
        });
    }

    /**
     * 检查奖池模式
     *
     * @param lib
     * @return
     */
    private boolean checkJackpool(FrozenThroneResultLib lib) {
        if (lib.jackpotEmpty()) {
            return false;
        }

        int count = 0;
        int jackpool = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == FrozenThroneConstant.BaseElement.ID_SCATTER) {
                count++;
            } else if (icon == FrozenThroneConstant.BaseElement.ID_MINI || icon == FrozenThroneConstant.BaseElement.ID_MINOR ||
                    icon == FrozenThroneConstant.BaseElement.ID_MAJOR || icon == FrozenThroneConstant.BaseElement.ID_GRAND) {
                jackpool++;
            }
        }
        return count >= 2 && jackpool > 0;
    }

    /**
     * 检查免费触发局
     *
     * @param lib
     * @return
     */
    private boolean checkTriggerFree(FrozenThroneResultLib lib) {
        int count = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == FrozenThroneConstant.BaseElement.ID_SCATTER) {
                count++;
            }
        }
        return count >= 3;
    }

    /**
     * 检查元素与小游戏所需要的参数是否匹配
     *
     * @param lib
     */
    private boolean checkElement(FrozenThroneResultLib lib) {
        if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            return true;
        }

        //检查二选一
        if (lib.getLibTypeSet().contains(FrozenThroneConstant.SpecialMode.FREE)
                && !checkTriggerFree(lib)) {
            log.warn("检查免费触发局失败");
            return false;
        }

        //检查jackpool模式
        if (lib.getLibTypeSet().contains(FrozenThroneConstant.SpecialMode.JACKPOOL) && !checkJackpool(lib)) {
            log.warn("检查jackpool模式失败");
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

            //增加免费次数
            if (cfg.getPlayType() == FrozenThroneConstant.SpecialPlay.TYPE_ADD_FREE_COUNT) {
                FrozenThroneAddFreeInfo tmpFrozenThroneAddFreeInfo = new FrozenThroneAddFreeInfo();

                String[] arr0 = cfg.getValue().split(",");

                tmpFrozenThroneAddFreeInfo.setLibType(Integer.parseInt(arr0[0]));
                tmpFrozenThroneAddFreeInfo.setTargetIcon(Integer.parseInt(arr0[1]));

                String[] arr1 = arr0[2].split("\\|");
                for (String frozenThroneAddFreeInfoStr : arr1) {
                    String[] arr2 = frozenThroneAddFreeInfoStr.split("_");

                    int times = Integer.parseInt(arr2[0]);
                    int addFreeCount = Integer.parseInt(arr2[1]);
                    int prop = Integer.parseInt(arr2[2]);

                    tmpFrozenThroneAddFreeInfo.addTimesInfo(times, addFreeCount, prop);
                }

                this.frozenThroneAddFreeInfo = tmpFrozenThroneAddFreeInfo;
            }
        }
    }

}
