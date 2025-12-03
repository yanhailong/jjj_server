package com.jjg.game.slots.game.wealthbank.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;
import com.jjg.game.slots.game.wealthbank.data.WealthBankAwardLineInfo;
import com.jjg.game.slots.game.wealthbank.data.WealthBankResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * @author 11
 * @date 2025/7/7 18:28
 */
@Component
public class WealthBankGenerateManager extends AbstractSlotsGenerateManager<WealthBankAwardLineInfo, WealthBankResultLib> {
    public WealthBankGenerateManager() {
        super(WealthBankResultLib.class);
    }

    //二选一相关的游戏id配置
    private Map<Integer, int[]> specialPlayAllBoardMap = null;
    //触发普通二选一，最少需要allboard个数
    private int allBoardMinCount = Integer.MAX_VALUE;

    //trainId -> auxiliaryId
    private Map<Integer, Integer> trainIdToAuxiliaryMap = null;

    @Override
    protected List<SpecialAuxiliaryInfo> assignPattern(WealthBankResultLib lib) {
        //获取指定图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_ASSIGN);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }

        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(lib.getIconArr());

        log.debug("[Wealth Bank] 检查指定图案");

        //小游戏
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();
        //已经出现的小游戏id
        Set<Integer> showAuxiliaryIdSet = new HashSet<>();
        addShowAuxiliaryId(lib, showAuxiliaryIdSet);


        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            //必须出现的图案
            Integer mustIconCount = showCountMap.get(cfg.getRewardNum());
            //条件图案总数量
            int elementAllCount = 0;
            for (int iconId : cfg.getElementId()) {
                Integer count = showCountMap.get(iconId);
                if (count != null) {
                    elementAllCount += count;
                }
            }

            //检查条件是否都满足
            if (mustIconCount == null || mustIconCount < 1 || elementAllCount < 1) {
                continue;
            }

            //是否触发小游戏
            if (cfg.getFeatureTriggerId() == null || cfg.getFeatureTriggerId().isEmpty()) {
                continue;
            }

            log.debug("[Wealth Bank] elementAllCount = {},showAuxiliaryIdSet = {},element = {}", elementAllCount, showAuxiliaryIdSet, cfg.getElementId());

            for (int i = 0; i < elementAllCount; i++) {
                cfg.getFeatureTriggerId().forEach(miniGameId -> {
                    if (!showAuxiliaryIdSet.contains(miniGameId)) { //如果没出现过的小游戏可以触发，拉火车除外
                        lib.getLibTypeSet().forEach(libType -> {
                            SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                            if (specialAuxiliaryInfo != null) {
                                SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(specialAuxiliaryInfo.getCfgId());

                                if (normalTrainsTrainIconId(specialAuxiliaryCfg.getType()) < 1) { //如果不是拉火车，则加入检测重复的名单里
                                    showAuxiliaryIdSet.add(miniGameId);
                                    log.debug("[Wealth Bank] showAuxiliaryIdSet 添加 miniGameId = {}", miniGameId);
                                }
                                specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                            }
                        });
                    }
                });
            }
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(WealthBankResultLib lib) {
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

        log.debug("[Wealth Bank] 检查全局分散");

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
            if (cfg.getFeatureTriggerId() == null || cfg.getFeatureTriggerId().isEmpty()) {
                continue;
            }

            cfg.getFeatureTriggerId().forEach(miniGameId -> {
                if (!showAuxiliaryIdSet.contains(miniGameId)) { //如果没出现过的小游戏可以触发，拉火车除外
                    lib.getLibTypeSet().forEach(libType -> {
                        SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                        if (specialAuxiliaryInfo != null) {
                            SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(specialAuxiliaryInfo.getCfgId());

                            if (normalTrainsTrainIconId(specialAuxiliaryCfg.getType()) < 1) { //如果不是拉火车，则加入检测重复的名单里
                                showAuxiliaryIdSet.add(miniGameId);
                            }
                            specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                        }
                    });
                }

            });
        }
        return specialAuxiliaryInfoList;
    }

    /**
     * 添加中奖线信息
     *
     * @param baseLineCfg
     * @param rewardCfg
     * @param sameCount
     * @param baseIconId
     * @param lineList
     * @param arr
     * @return
     */
    @Override
    protected WealthBankAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount,
                                                       int baseIconId, List<Integer> lineList, int[] arr) {
        WealthBankAwardLineInfo awardLineInfo = new WealthBankAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);

//                    slotsResultLib.addTimes(rewardCfg.getBet());
//                    log.debug("[Wealth Bank] 中奖！！ 添加基础倍率 lineId = {},sameCount = {},addTimes = {}", cfg.getLineId(), sameCount, rewardCfg.getBet());

        for (List<Integer> otherIconList : rewardCfg.getBetTimes()) {
            int iconId = otherIconList.get(0);
            //该元素在这条线上出现的次数
            long showCount = lineList.stream().filter(tmpId -> arr[tmpId] == iconId).count();
            if (showCount == otherIconList.get(1)) {
                int addTimes = otherIconList.get(2);
                awardLineInfo.addSpecialAwardInfo(iconId, addTimes);
//                            log.debug("[Wealth Bank] 特殊图标添加倍率 iconId = {},showCount = {},addTimes = {}", iconId, showCount, addTimes);
            }
        }
        return awardLineInfo;
    }

    /**
     * 投资游戏中，3次选择地图的中奖倍数
     *
     * @param specialAuxiliaryCfg
     * @return
     */
    public List<Integer> inversTimes(SpecialAuxiliaryCfg specialAuxiliaryCfg) {
        if (specialAuxiliaryCfg == null) {
            return Collections.emptyList();
        }

        SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(specialAuxiliaryCfg.getId());
        if (specialAuxiliaryPropConfig == null) {
            return Collections.emptyList();
        }

        //获取随机次数
        Integer randCount = specialAuxiliaryPropConfig.getRandCountPropInfo().getRandKey();
        if (randCount == null || randCount < 1) {
            return Collections.emptyList();
        }

        //因为有最大次数限制，所以先clone
        PropInfo clonePropInfo = specialAuxiliaryPropConfig.getAwardTypeCPropInfo().clone();
        //出现的次数记录
        Map<Integer, Integer> showMap = new HashMap<>();

        List<Integer> timesList = new ArrayList<>(randCount);
        for (int i = 0; i < randCount; i++) {
            Integer times = clonePropInfo.getRandKey();
            if (times == null) {
                continue;
            }
            showMap.merge(times, 1, Integer::sum);

            timesList.add(times);

            //达到最大次数限制后，移除
            if (showMap.get(times) >= clonePropInfo.getMaxShowLimit(times)) {
                clonePropInfo.removeKeyAndRecalculate(times);
            }
        }
        return timesList;
    }

    /**
     * 投资游戏，选地图3次全中奖获取黄金列车节数
     *
     * @param specialAuxiliaryCfg
     * @return
     */
    public int inversAllWinGoldTrainCount(SpecialAuxiliaryCfg specialAuxiliaryCfg) {
        if (specialAuxiliaryCfg == null) {
            return 0;
        }

        SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(specialAuxiliaryCfg.getId());
        if (specialAuxiliaryPropConfig == null) {
            return 0;
        }

        Integer count = specialAuxiliaryPropConfig.getRandCountPropInfo().getRandKey();
        if (count == null) {
            return 0;
        }
        return count;
    }


    /**********************************************************************************************/

    /**
     * 计算倍数
     *
     * @param lib
     */
    public void calTimes(WealthBankResultLib lib) throws Exception {
        if (!checkElement(lib)) {
            log.warn("[Wealth Bank] lib = {}", JSONObject.toJSONString(lib));
            throw new IllegalArgumentException("检查结果有错误");
        }

        sortTrain(lib);
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //普通火车
        lib.addTimes(calNormalTrain(lib));
        //黄金火车
        lib.addTimes(calGoldTrain(lib));
        //保险箱
        lib.addTimes(calSafeBox(lib));
        //免费
        lib.addTimes(calFree(lib));
    }

    /**
     * 检查元素与小游戏所需要的参数是否匹配
     *
     * @param lib
     */
    private boolean checkElement(WealthBankResultLib lib) {
        if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            return true;
        }

        //检查拉火车
        if (lib.getLibTypeSet().contains(WealthBankConstant.SpecialMode.TYPE_TRIGGER_NORMAL_TRAIN)
                && !checkNormalTrain(lib)) {
            log.info("[Wealth Bank] 检查普通火车失败");
            return false;
        }


        //检查黄金列车
        if (lib.getLibTypeSet().contains(WealthBankConstant.SpecialMode.TYPE_TRIGGER_GOLD_TRAIN) && !checkGoldTrainIcon(lib.getIconArr())) {
            log.info("[Wealth Bank] 检查黄金列车失败");
            return false;
        }

        //检查二选一
        if (lib.getLibTypeSet().contains(WealthBankConstant.SpecialMode.TYPE_TRIGGER_ALL_BOARD) && !checkAllBoard(lib.getIconArr())) {
            log.info("[Wealth Bank] 检查二选一失败");
            return false;
        }

        //检查保险箱
        if (lib.getLibTypeSet().contains(WealthBankConstant.SpecialMode.TYPE_TRIGGER_SAFE_BOX) && !checkSafeBox(lib)) {
            log.info("[Wealth Bank] 检查保险箱失败");
            return false;
        }

        //检查免费模式
        if (lib.getLibTypeSet().contains(WealthBankConstant.SpecialMode.TYPE_TRIGGER_FREE) && !checkFreeIcon(lib.getIconArr())) {
            log.info("[Wealth Bank] 检查免费模式失败");
            return false;
        }
        return true;
    }

    /**
     * 火车排序
     *
     * @param lib
     * @return
     */
    private void sortTrain(WealthBankResultLib lib) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return;
        }

        //根据火车出现的顺序，排列auxiliaryId的顺序
        List<Integer> auxiliaryIdList = new ArrayList<>();
        for (int i = 1; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (trainId(icon)) {
                auxiliaryIdList.add(this.trainIdToAuxiliaryMap.get(icon));
            }
        }

        if (auxiliaryIdList.isEmpty()) {
            return;
        }

        //将specialAuxiliaryInfoList转化成map映射
        Map<Integer, List<SpecialAuxiliaryInfo>> tmpAuxiliaryMap = new HashMap<>();
        lib.getSpecialAuxiliaryInfoList().forEach(specialAuxiliaryInfo -> {
            List<SpecialAuxiliaryInfo> tmpList = tmpAuxiliaryMap.computeIfAbsent(specialAuxiliaryInfo.getCfgId(), k -> new ArrayList<>());
            tmpList.add(specialAuxiliaryInfo);
//            tmpAuxiliaryMap.put(specialAuxiliaryInfo.getCfgId(), specialAuxiliaryInfo);
        });

        //根据顺序重新放入新的list
        List<SpecialAuxiliaryInfo> sortList = new ArrayList<>();
        for (int auxiliaryId : auxiliaryIdList) {
            List<SpecialAuxiliaryInfo> tmpList = tmpAuxiliaryMap.get(auxiliaryId);
            if(tmpList == null){
                continue;

            }
            SpecialAuxiliaryInfo info = tmpList.removeFirst();
            if(info == null){
                continue;
            }
            sortList.add(info);
        }

        //将剩余的 SpecialAuxiliaryInfo 放入sortList中
        if(!tmpAuxiliaryMap.isEmpty()){
            tmpAuxiliaryMap.forEach((k,v) -> {
                sortList.addAll(v);
            });
        }

        lib.setSpecialAuxiliaryInfoList(sortList);
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    private int calLineTimes(List<WealthBankAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (WealthBankAwardLineInfo awardLineInfo : list) {
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
     * 计算小游戏里面的倍数
     *
     * @return
     */
    private long calNormalTrain(WealthBankResultLib lib) {
        if (!checkNormalTrain(lib)) {
            return 0;
        }

        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }
        long times = 0;
        for (SpecialAuxiliaryInfo info : lib.getSpecialAuxiliaryInfoList()) {
            SpecialAuxiliaryCfg cfg = GameDataManager.getSpecialAuxiliaryCfg(info.getCfgId());
            if (cfg == null) {
                continue;
            }

            if (normalTrainsTrainIconId(cfg.getType()) > 0) {  //拉火车
                List<SpecialAuxiliaryAwardInfo> awardInfos = info.getAwardInfos();
                if (awardInfos == null || awardInfos.isEmpty()) {
                    continue;
                }

                for (SpecialAuxiliaryAwardInfo awardInfo : awardInfos) {
                    if (awardInfo.getAwardCList() != null && !awardInfo.getAwardCList().isEmpty()) {
                        times += awardInfo.getAwardCList().stream().reduce(0, Integer::sum);
                    }
                }
            }
        }
        return times;
    }

    /**
     * 计算保险箱的倍数
     *
     * @param lib
     * @return
     */
    private long calSafeBox(WealthBankResultLib lib) {
        if (!checkSafeBox(lib)) {
            return 0;
        }

        if (lib.getSpecialGirdInfoList() == null || lib.getSpecialGirdInfoList().isEmpty()) {
            return 0;
        }
        long times = 0;
        for (SpecialGirdInfo specialGirdInfo : lib.getSpecialGirdInfoList()) {
            if (specialGirdInfo.getValueMap() == null || specialGirdInfo.getValueMap().isEmpty()) {
                continue;
            }
            for (Map.Entry<Integer, Integer> en : specialGirdInfo.getValueMap().entrySet()) {
                times += en.getValue();
            }
        }
        return times;
    }

    /**
     * 计算黄金列车倍数
     *
     * @return
     */
    private long calGoldTrain(WealthBankResultLib lib) throws Exception {
        if (!checkGoldTrainIcon(lib.getIconArr())) {
            return 0;
        }
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty() ||
                lib.getSpecialGirdInfoList() == null || lib.getSpecialGirdInfoList().isEmpty()) {
            return 0;
        }
        SpecialAuxiliaryAwardInfo specialAuxiliaryAwardInfo = (SpecialAuxiliaryAwardInfo) lib.getSpecialAuxiliaryInfoList().get(0).getAwardInfos().get(0);

        long times = 0;
        for (SpecialGirdInfo specialGirdInfo : lib.getSpecialGirdInfoList()) {
            if (specialGirdInfo.getValueMap() == null || specialGirdInfo.getValueMap().isEmpty()) {
                continue;
            }

            for (Map.Entry<Integer, Integer> en : specialGirdInfo.getValueMap().entrySet()) {
                times += en.getValue();
            }
        }

        return times * specialAuxiliaryAwardInfo.getRandCount();
    }

    /**
     * 计算免费游戏的总倍数
     *
     * @param lib
     * @return
     */
    private long calFree(WealthBankResultLib lib) throws Exception {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }

        long times = 0;
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                continue;
            }

            for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                WealthBankResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), WealthBankResultLib.class);
                calTimes(tmpLib);
                times += tmpLib.getTimes();
            }
        }
        return times;
    }

    /**
     * 检查普通火车的icon
     *
     * @param arr
     * @return
     */
    public int checkNormalTrainIcon(int[] arr) {
        //前面4列
        int trainCount = 0;
        for (int i = 1; i <= 16; i++) {
            int icon = arr[i];
            if (trainId(icon)) {
                trainCount++;
            }
        }

        boolean safeBox = false;
        //最后1列
        for (int i = 17; i <= 20; i++) {
            int icon = arr[i];
            if (icon == WealthBankConstant.BaseElement.ID_SAFE_BOX) {
                safeBox = true;
            }
        }

        if (trainCount > 0 && safeBox) {
            return trainCount;
        }
        return 0;
    }

    /**
     * 检查普通火车
     *
     * @param lib
     * @return
     */
    public boolean checkNormalTrain(WealthBankResultLib lib) {
        int trainCount = checkNormalTrainIcon(lib.getIconArr());
        if (trainCount < 1) {
            return false;
        }

        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            log.debug("[Wealth Bank] 满足火车开启条件，但是没有数据1");
            return false;
        }

        int trainDataCount = 0;
        for (SpecialAuxiliaryInfo info : lib.getSpecialAuxiliaryInfoList()) {
            SpecialAuxiliaryCfg cfg = GameDataManager.getSpecialAuxiliaryCfg(info.getCfgId());
            int icon = normalTrainsTrainIconId(cfg.getType());
            if (icon > 0) {
                trainDataCount++;
            }
        }

        if (trainDataCount != trainCount) {
            log.debug("[Wealth Bank] 火车数据对不上 trainDataCount = {}, trainCount = {}", trainDataCount, trainCount);
            return false;
        }
        return true;
    }

    /**
     * 检查黄金列车的icon
     *
     * @param arr
     * @return
     */
    public boolean checkGoldTrainIcon(int[] arr) {
        //前面4列
        int dollarCount = 0;
        for (int i = 1; i <= 16; i++) {
            int icon = arr[i];
            if (icon == WealthBankConstant.BaseElement.ID_DOLLAR || icon == WealthBankConstant.BaseElement.ID_DOLLAR_2) {
                dollarCount++;
            }
        }

        boolean goldTrain = false;
        //最后1列
        for (int i = 17; i <= 20; i++) {
            int icon = arr[i];
            if (icon == WealthBankConstant.BaseElement.ID_GOLD_TRAIN) {
                goldTrain = true;
            }
        }

        if (dollarCount < 1 || !goldTrain) {
            return false;
        }
        return true;
    }

    /**
     * 检查all board
     *
     * @param arr
     * @return
     */
    public boolean checkAllBoard(int[] arr) {
        int count = allBoadrCount(arr);
        if (count < this.allBoardMinCount) {
            return false;
        }
        return true;
    }

    public int allBoadrCount(int[] arr) {
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            int icon = arr[i];
            if (icon == WealthBankConstant.BaseElement.ID_ALL_ABOARD) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查现金奖励的icon
     *
     * @param arr
     * @return
     */
    public boolean checkSafeBoxIcon(int[] arr) {
        //前面4列
        int dollarCount = 0;
        for (int i = 1; i <= 16; i++) {
            int icon = arr[i];
            if (icon == WealthBankConstant.BaseElement.ID_DOLLAR || icon == WealthBankConstant.BaseElement.ID_DOLLAR_2) {
                dollarCount++;
            }
        }

        boolean safeBox = false;
        //最后1列
        for (int i = 17; i <= 20; i++) {
            int icon = arr[i];
            if (icon == WealthBankConstant.BaseElement.ID_SAFE_BOX) {
                safeBox = true;
            }
        }

        if (dollarCount < 1 || !safeBox) {
            return false;
        }
        return true;
    }

    /**
     * 检查保险箱
     *
     * @param lib
     * @return
     */
    public boolean checkSafeBox(WealthBankResultLib lib) {
        boolean safeBoxIcon = checkSafeBoxIcon(lib.getIconArr());
        if (!safeBoxIcon) {
            return false;
        }

        if (lib.getSpecialGirdInfoList() == null || lib.getSpecialGirdInfoList().isEmpty()) {
            log.debug("[Wealth Bank] 满足美元现金奖励，但是没有数据");
            return false;
        }

        for (SpecialGirdInfo specialGirdInfo : lib.getSpecialGirdInfoList()) {
            if (specialGirdInfo.getValueMap() == null || specialGirdInfo.getValueMap().isEmpty()) {
                log.debug("[Wealth Bank] 满足美元现金奖励，但是没有数据2");
                return false;
            }
        }
        return true;
    }

    public boolean checkFreeIcon(int[] arr) {
        int freeCount = 0;
        for (int i = 0; i < arr.length; i++) {
            int icon = arr[i];
            if (icon == WealthBankConstant.BaseElement.ID_FREE) {
                freeCount++;
            }
        }

        if (freeCount < 1) {
            return false;
        }
        return true;
    }

    /**
     * 是否为火车id
     *
     * @param iconId
     * @return
     */
    public boolean trainId(int iconId) {
        if (iconId == WealthBankConstant.BaseElement.ID_GREEN_TRAIN ||
                iconId == WealthBankConstant.BaseElement.ID_RED_TRAIN ||
                iconId == WealthBankConstant.BaseElement.ID_BLUE_TRAIN ||
                iconId == WealthBankConstant.BaseElement.ID_PURPLE_TRAIN) {
            return true;
        }
        return false;
    }

    @Override
    protected void baseElementRewardConfig() {
        super.baseElementRewardConfig();

        //火车id与specialAuxiliary id对应
        Map<Integer, BaseElementRewardCfg> typeAssignTmpMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_ASSIGN);
        if (typeAssignTmpMap != null && !typeAssignTmpMap.isEmpty()) {
            Map<Integer, Integer> tmpTrainIdToAuxiliaryMap = new HashMap<>();
            typeAssignTmpMap.forEach((k, v) -> {
                tmpTrainIdToAuxiliaryMap.put(v.getElementId().get(0), v.getFeatureTriggerId().get(0));
            });
            this.trainIdToAuxiliaryMap = tmpTrainIdToAuxiliaryMap;
        }

        //触发普通二选一，最少需要allboard个数
        Map<Integer, BaseElementRewardCfg> typeGolbalTmpMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (typeGolbalTmpMap != null && !typeGolbalTmpMap.isEmpty()) {
            int tmpAllBoardMinCount = Integer.MAX_VALUE;
            for (Map.Entry<Integer, BaseElementRewardCfg> en : typeGolbalTmpMap.entrySet()) {
                BaseElementRewardCfg cfg = en.getValue();
                if (cfg.getElementId().contains(WealthBankConstant.BaseElement.ID_ALL_ABOARD)) {
                    if (cfg.getRewardNum() < tmpAllBoardMinCount) {
                        tmpAllBoardMinCount = cfg.getRewardNum();
                    }
                }
            }
            this.allBoardMinCount = tmpAllBoardMinCount;
        }
    }

    @Override
    protected void specialPlayConfig() {
        Map<Integer, int[]> tmpSpecialPlayAllBoardMap = new HashMap<>();

        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            //投资小游戏
            if (cfg.getPlayType() == WealthBankConstant.SpecialPlay.TYPE_ALL_BOARD) {
                String[] arr = cfg.getValue().split("\\|");
                for (String s : arr) {
                    String[] arr1 = s.split(",");

                    String[] s1 = arr1[1].split("_");

                    tmpSpecialPlayAllBoardMap.put(Integer.parseInt(arr1[0]), new int[]{Integer.parseInt(s1[0]), Integer.parseInt(s1[1])});
                }
            }
        }

        this.specialPlayAllBoardMap = tmpSpecialPlayAllBoardMap;
    }


    public Map<Integer, int[]> getSpecialPlayAllBoardMap() {
        return specialPlayAllBoardMap;
    }

    public int getAllBoardMinCount() {
        return allBoardMinCount;
    }

    public int allTrainsTrainIconId(int auxiliaryType) {
        int icon = normalTrainsTrainIconId(auxiliaryType);
        if (icon > 0) {
            return icon;
        }

        if (auxiliaryType == WealthBankConstant.SpecialAuxiliary.TYPE_GOLD_TRAIN) {
            return WealthBankConstant.BaseElement.ID_GOLD_TRAIN;
        }
        return 0;
    }

    public int goldTrainsTrainIconId(int auxiliaryType) {
        if (auxiliaryType == WealthBankConstant.SpecialAuxiliary.TYPE_GOLD_TRAIN) {
            return WealthBankConstant.BaseElement.ID_GOLD_TRAIN;
        }
        return 0;
    }

    /**
     * specialAuxiliary 中的火车type与icon转化
     *
     * @param auxiliaryType
     * @return
     */
    public int normalTrainsTrainIconId(int auxiliaryType) {
        if (auxiliaryType == WealthBankConstant.SpecialAuxiliary.TYPE_GREEN_TRAIN) {
            return WealthBankConstant.BaseElement.ID_GREEN_TRAIN;
        }
        if (auxiliaryType == WealthBankConstant.SpecialAuxiliary.TYPE_BLUE_TRAIN) {
            return WealthBankConstant.BaseElement.ID_BLUE_TRAIN;
        }
        if (auxiliaryType == WealthBankConstant.SpecialAuxiliary.TYPE_PUEPLE_TRAIN) {
            return WealthBankConstant.BaseElement.ID_PURPLE_TRAIN;
        }
        if (auxiliaryType == WealthBankConstant.SpecialAuxiliary.TYPE_RED_TRAIN) {
            return WealthBankConstant.BaseElement.ID_RED_TRAIN;
        }
        return 0;
    }

    /**
     * 添加已经出现的小游戏id
     *
     * @param lib
     * @param set
     */
    private void addShowAuxiliaryId(WealthBankResultLib lib, Set<Integer> set) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return;
        }

        lib.getSpecialAuxiliaryInfoList().forEach(info -> {
            SpecialAuxiliaryCfg cfg = GameDataManager.getSpecialAuxiliaryCfg(info.getCfgId());
            if (normalTrainsTrainIconId(cfg.getType()) < 1) {  //拉火车可以重复
                set.add(info.getCfgId());
            }
        });
    }
}
