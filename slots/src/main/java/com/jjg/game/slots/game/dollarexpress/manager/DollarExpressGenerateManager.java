package com.jjg.game.slots.game.dollarexpress.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.*;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * @author 11
 * @date 2025/7/7 18:28
 */
@Component
public class DollarExpressGenerateManager extends AbstractSlotsGenerateManager<DollarExpressAwardLineInfo, DollarExpressResultLib> {
    public DollarExpressGenerateManager() {
        super(DollarExpressResultLib.class);
    }

    //二选一相关的游戏id配置
    private Map<Integer, int[]> specialPlayAllBoardMap = null;
    //触发普通二选一，最少需要allboard个数
    private int allBoardMinCount = Integer.MAX_VALUE;

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
    protected DollarExpressAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount,
                                                          int baseIconId, List<Integer> lineList, int[] arr) {
        DollarExpressAwardLineInfo awardLineInfo = new DollarExpressAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);

//                    slotsResultLib.addTimes(rewardCfg.getBet());
//                    log.debug("中奖！！ 添加基础倍率 lineId = {},sameCount = {},addTimes = {}", cfg.getLineId(), sameCount, rewardCfg.getBet());

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
    public void calTimes(DollarExpressResultLib lib) throws Exception {
        if (!checkElement(lib)) {
            log.warn("lib = {}", JSONObject.toJSONString(lib));
            throw new IllegalArgumentException("检查结果有错误");
        }

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
    private boolean checkElement(DollarExpressResultLib lib) {
        if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            return true;
        }

        //检查拉火车
        if (lib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_NORMAL_TRAIN)
                && !checkNormalTrain(lib)) {
            log.info("检查普通火车失败");
            return false;
        }


        //检查黄金列车
        if (lib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_GOLD_TRAIN) && !checkGoldTrainIcon(lib.getIconArr())) {
            log.info("检查黄金列车失败");
            return false;
        }

        //检查二选一
        if (lib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_ALL_BOARD) && !checkAllBoard(lib.getIconArr())) {
            log.info("检查二选一失败");
            return false;
        }

        //检查保险箱
        if (lib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_SAFE_BOX) && !checkSafeBox(lib)) {
            log.info("检查保险箱失败");
            return false;
        }

        //检查免费模式
        if (lib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_FREE) && !checkFreeIcon(lib.getIconArr())) {
            log.info("检查免费模式失败");
            return false;
        }
        return true;
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    private int calLineTimes(List<DollarExpressAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (DollarExpressAwardLineInfo awardLineInfo : list) {
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
    private long calNormalTrain(DollarExpressResultLib lib) {
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
    private long calSafeBox(DollarExpressResultLib lib) {
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
    private long calGoldTrain(DollarExpressResultLib lib) throws Exception {
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
    private long calFree(DollarExpressResultLib lib) throws Exception {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }

        long times = 0;
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                continue;
            }

            for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                DollarExpressResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), DollarExpressResultLib.class);
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
            if (icon == DollarExpressConstant.BaseElement.ID_SAFE_BOX) {
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
    public boolean checkNormalTrain(DollarExpressResultLib lib) {
        int trainCount = checkNormalTrainIcon(lib.getIconArr());
        if (trainCount < 1) {
            return false;
        }

        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            log.debug("满足火车开启条件，但是没有数据1");
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
            log.debug("火车数据对不上");
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
            if (icon == DollarExpressConstant.BaseElement.ID_DOLLAR || icon == DollarExpressConstant.BaseElement.ID_DOLLAR_2) {
                dollarCount++;
            }
        }

        boolean goldTrain = false;
        //最后1列
        for (int i = 17; i <= 20; i++) {
            int icon = arr[i];
            if (icon == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN) {
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
            if (icon == DollarExpressConstant.BaseElement.ID_ALL_ABOARD) {
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
            if (icon == DollarExpressConstant.BaseElement.ID_DOLLAR || icon == DollarExpressConstant.BaseElement.ID_DOLLAR_2) {
                dollarCount++;
            }
        }

        boolean safeBox = false;
        //最后1列
        for (int i = 17; i <= 20; i++) {
            int icon = arr[i];
            if (icon == DollarExpressConstant.BaseElement.ID_SAFE_BOX) {
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
    public boolean checkSafeBox(DollarExpressResultLib lib) {
        boolean safeBoxIcon = checkSafeBoxIcon(lib.getIconArr());
        if (!safeBoxIcon) {
            return false;
        }

        if (lib.getSpecialGirdInfoList() == null || lib.getSpecialGirdInfoList().isEmpty()) {
            log.debug("满足美元现金奖励，但是没有数据");
            return false;
        }

        for (SpecialGirdInfo specialGirdInfo : lib.getSpecialGirdInfoList()) {
            if (specialGirdInfo.getValueMap() == null || specialGirdInfo.getValueMap().isEmpty()) {
                log.debug("满足美元现金奖励，但是没有数据2");
                return false;
            }
        }
        return true;
    }

    public boolean checkFreeIcon(int[] arr) {
        int freeCount = 0;
        for (int i = 0; i < arr.length; i++) {
            int icon = arr[i];
            if (icon == DollarExpressConstant.BaseElement.ID_FREE) {
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
        if (iconId == DollarExpressConstant.BaseElement.ID_GREEN_TRAIN ||
                iconId == DollarExpressConstant.BaseElement.ID_RED_TRAIN ||
                iconId == DollarExpressConstant.BaseElement.ID_BLUE_TRAIN ||
                iconId == DollarExpressConstant.BaseElement.ID_PURPLE_TRAIN) {
            return true;
        }
        return false;
    }

    @Override
    protected void baseElementRewardConfig() {
        super.baseElementRewardConfig();
        if (this.baseElementRewardCfgMap == null || this.baseElementRewardCfgMap.isEmpty()) {
            return;
        }

        Map<Integer, BaseElementRewardCfg> tmpMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return;
        }

        int tmpAllBoardMinCount = Integer.MAX_VALUE;
        for (Map.Entry<Integer, BaseElementRewardCfg> en : tmpMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            if (cfg.getElementId().contains(DollarExpressConstant.BaseElement.ID_ALL_ABOARD)) {
                if (cfg.getRewardNum() < tmpAllBoardMinCount) {
                    tmpAllBoardMinCount = cfg.getRewardNum();
                }
            }
        }
        this.allBoardMinCount = tmpAllBoardMinCount;
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
            if (cfg.getPlayType() == DollarExpressConstant.SpecialPlay.TYPE_ALL_BOARD) {
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

        if (auxiliaryType == DollarExpressConstant.SpecialAuxiliary.TYPE_GOLD_TRAIN) {
            return DollarExpressConstant.BaseElement.ID_GOLD_TRAIN;
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
        if (auxiliaryType == DollarExpressConstant.SpecialAuxiliary.TYPE_GREEN_TRAIN) {
            return DollarExpressConstant.BaseElement.ID_GREEN_TRAIN;
        }
        if (auxiliaryType == DollarExpressConstant.SpecialAuxiliary.TYPE_BLUE_TRAIN) {
            return DollarExpressConstant.BaseElement.ID_BLUE_TRAIN;
        }
        if (auxiliaryType == DollarExpressConstant.SpecialAuxiliary.TYPE_PUEPLE_TRAIN) {
            return DollarExpressConstant.BaseElement.ID_PURPLE_TRAIN;
        }
        if (auxiliaryType == DollarExpressConstant.SpecialAuxiliary.TYPE_RED_TRAIN) {
            return DollarExpressConstant.BaseElement.ID_RED_TRAIN;
        }
        return 0;
    }
}
