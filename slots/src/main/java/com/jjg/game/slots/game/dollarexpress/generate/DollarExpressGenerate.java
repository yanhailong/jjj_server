package com.jjg.game.slots.game.dollarexpress.generate;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.slots.constant.AuxiliaryAwardType;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.*;
import com.jjg.game.slots.generate.SlotsGenerate;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author 11
 * @date 2025/7/7 18:28
 */
public class DollarExpressGenerate extends SlotsGenerate<DollarExpressResultLib> {
    public DollarExpressGenerate(int gameType) {
        super(DollarExpressResultLib.class, gameType);
    }

    //因为小游戏奖励可能会触发另外一个小游戏，所以这里有递归跳出条件
    private int auxiliaryAecursionCount = 0;

    private int currentFreeGameId;
    private int currentAgainGameId;

    private DollarCashConfig dollarCashConfig;
    //随机倍数
    private Map<Integer, PropInfo> iconTimesPropMap;

    @Override
    public void initConfig() {
        super.initConfig();

        specialPlayConfig();
    }

    /**
     * 生成一条结果
     */
    @Override
    public void generateOne() {
        try {
            //获取模式id和滚轴id
            DollarExpressResultLib slotsResultLib = randRollerId();

            //生成20个图标
            int[] arr = generateAllIcons(slotsResultLib.getRollerId());
            if (arr == null) {
                return;
            }

            checkAward(arr,slotsResultLib);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public DollarExpressResultLib checkAward(int[] arr,DollarExpressResultLib slotsResultLib) throws Exception {
        this.auxiliaryAecursionCount = 0;
        this.currentFreeGameId = 0;
        this.currentAgainGameId = 0;
        this.branchLibMap = new HashMap<>();

        slotsResultLib.setId(RandomUtils.getUUid());
        slotsResultLib.setLibType(SlotsConst.SpecialResultLib.TYPE_NORMAL);
        this.branchLibMap.put(SlotsConst.Common.TYPE_TRIGGER,slotsResultLib);

        slotsResultLib.setGameType(this.gameType);
        slotsResultLib.setIconArr(arr);

        //检查中奖线
        List<DollarExpressAwardLineInfo> awardLineInfoList = normalAward(slotsResultLib.getIconArr(), SlotsConst.BaseElementReward.ROTATESTATE_NORMAL);
        slotsResultLib.setAwardLineInfoList(awardLineInfoList);

        //检查特殊奖励
        slotsResultLib = specialAward(arr, slotsResultLib, SlotsConst.BaseElementReward.ROTATESTATE_NORMAL);

        for(Map.Entry<Integer,DollarExpressResultLib> en : this.branchLibMap.entrySet()){
            calTimes(en.getValue());
        }
        return slotsResultLib;
    }

    /**
     * 检查普通奖励
     *
     * @param arr
     * @param rotateState 旋转状态
     * @return
     */
    public List<DollarExpressAwardLineInfo> normalAward(int[] arr, int rotateState) {
        log.debug("开始检查中奖线信息 rotateState = {}", rotateState);
        List<DollarExpressAwardLineInfo> awardLineInfoList = new ArrayList<>();
        // 统计arr中每个元素的出现次数
        Map<Integer, Integer> iconShowCountMap = iconShowCount(arr);

        for (Map.Entry<Integer, BaseLineCfg> en : this.baseLineCfgMap.entrySet()) {
            BaseLineCfg cfg = en.getValue();
            int id = cfg.getGamePlay().get(0).get(0);
            List<Integer> lineList = cfg.getPosLocation().get(id);

            SameInfo sameInfo = new SameInfo();

            int last = lineList.size() - 1;

            //标记是否连线
            int sameCount = 0;

            for (int i = 0; i < last; i++) {
                int index1 = lineList.get(i);

                int behind = i + 1;
                int index2 = lineList.get(behind);

//                log.debug("index1={}, index2={}", index1, index2);
                sameInfo = iconSame(sameInfo, arr[index1], arr[index2]);
                if (sameInfo.isSame()) {
                    sameInfo.setSame(false);
                    sameCount = sameCount < 1 ? 2 : sameCount + 1;
                } else {
                    break;
                }
            }

            //如果有连线
            if (sameCount > 1) {
                Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_NORMAL);
                for (Map.Entry<Integer, BaseElementRewardCfg> rewardEn : normalRewardCfgMap.entrySet()) {
                    BaseElementRewardCfg rewardCfg = rewardEn.getValue();
                    if (rewardCfg.getRotateState() != SlotsConst.BaseElementReward.ROTATESTATE_ALL && rewardCfg.getRotateState() != rotateState) {
                        continue;
                    }

                    //匹配连线的元素id和个数
                    if (rewardCfg.getElementId() != sameInfo.getBaseIconId() || sameCount != rewardCfg.getRewardNum()) {
                        continue;
                    }

                    DollarExpressAwardLineInfo awardLineInfo = new DollarExpressAwardLineInfo();
                    awardLineInfo.setId(cfg.getLineId());
                    awardLineInfo.setBaseTimes(rewardCfg.getBet());
                    awardLineInfo.setSameCount(sameCount);
                    awardLineInfo.setIconId(sameInfo.getBaseIconId());

//                    slotsResultLib.addTimes(rewardCfg.getBet());
                    log.debug("中奖！！ 添加基础倍率 lineId = {},sameCount = {},addTimes = {}", cfg.getLineId(), sameCount, rewardCfg.getBet());

                    for (List<Integer> otherIconList : rewardCfg.getBetTimes()) {
                        int iconId = otherIconList.get(0);
                        //出现的次数
                        Integer showCount = iconShowCountMap.get(iconId);
                        if (showCount != null && showCount == otherIconList.get(1)) {
                            int addTimes = otherIconList.get(2);
                            awardLineInfo.addSpecialAwardInfo(iconId, addTimes);
                            log.debug("特殊图标添加倍率 iconId = {},showCount = {},addTimes = {}", iconId, showCount, addTimes);
                        }
                    }
                    awardLineInfoList.add(awardLineInfo);
                    break;
                }
            }
        }
        return awardLineInfoList;
    }

    /**
     * 特殊奖励
     *
     * @param slotsResultLib
     * @param rotateState
     * @return
     */
    public DollarExpressResultLib specialAward(int[] iconArr, DollarExpressResultLib slotsResultLib, int rotateState) throws Exception {
        log.debug("开始检查特殊中奖");
        // 统计arr中每个元素的出现次数
        Map<Integer, Integer> iconShowCountMap = iconShowCount(iconArr);

        //检查baseLineFree配置的出现的特殊元素，  线路玩法 -> 主元素id -> 出现总次数
        Map<Integer, Map<Integer, Integer>> showIdMap = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, BaseLineFreeInfo>> en : this.baseLineFreeCfgMap.entrySet()) {
            Map<Integer, BaseLineFreeInfo> freeInfoMap = en.getValue();
            for (Map.Entry<Integer, BaseLineFreeInfo> en2 : freeInfoMap.entrySet()) {
                BaseLineFreeInfo freeInfo = en2.getValue();
                Map<Integer, Integer> tempMap = showIdMap.computeIfAbsent(freeInfo.getPlayType(), k -> new HashMap<>());

                group:
                for (List<Integer> groupList : freeInfo.getElementGroupList()) {
                    for (int specialIcon : groupList) {
                        Integer count = iconShowCountMap.get(specialIcon);
                        if (count == null) {
                            tempMap.remove(freeInfo.getMainElementId());
                            continue group;
                        }
                        tempMap.merge(freeInfo.getMainElementId(), count, Integer::sum);
                    }
                }
            }
        }

        for (Map.Entry<Integer, Map<Integer, BaseElementRewardCfg>> specialElementCfgEn : this.baseElementRewardCfgMap.entrySet()) {
            //线路玩法
            int playType = specialElementCfgEn.getKey();
            //只检测特殊线路玩法
            if (playType == SlotsConst.BaseElementReward.LINE_TYPE_NORMAL) {
                continue;
            }

            //从 baseLineFree 读取icon的数据
            Map<Integer, Integer> tempShowIdMap = null;
            if (!showIdMap.isEmpty()) {
                tempShowIdMap = showIdMap.get(playType);
            }

            Map<Integer, BaseElementRewardCfg> specialElementCfgMap = specialElementCfgEn.getValue();
            for (Map.Entry<Integer, BaseElementRewardCfg> en : specialElementCfgMap.entrySet()) {
                BaseElementRewardCfg cfg = en.getValue();
                //元素id
                int iconId = cfg.getElementId();
                //检查tempShowIdMap是否有特殊元素的个数

                //匹配连线的元素id和个数
                Integer showCount = null;

                if (tempShowIdMap != null && !tempShowIdMap.isEmpty()) {
                    showCount = tempShowIdMap.get(iconId);
                }

                if (showCount == null) {
                    showCount = iconShowCountMap.get(iconId);
                    if (showCount == null) {
                        continue;
                    }
                }

                //出现的个数没有达到要求
                if (showCount != cfg.getRewardNum()) {
                    continue;
                }

                //是否有小游戏配置
                Map<Integer, Integer> featureTriggerIdMap = cfg.getFeatureTriggerId();
                if (featureTriggerIdMap == null || featureTriggerIdMap.isEmpty()) {
                    continue;
                }

                //小游戏id
                Integer miniGameId = featureTriggerIdMap.get(slotsResultLib.getRollerMode());
                if (miniGameId == null || miniGameId < 1) {
                    continue;
                }
                slotsResultLib = triggerMiniGame(slotsResultLib, miniGameId, rotateState);
            }
        }

        //美元现金
        DollarInfo dollarInfo = checkDollarCash(iconArr);
        if (dollarInfo != null) {
            if (rotateState == SlotsConst.BaseElementReward.ROTATESTATE_FREE) {
                slotsResultLib.addFreeGameDollarCashInfo(this.currentFreeGameId, dollarInfo);
            } else {
                slotsResultLib.setDollarCashInfo(dollarInfo);
            }
        }

        return slotsResultLib;
    }

    /**
     * 检查美元
     *
     * @param iconArr
     * @return
     */
    public DollarInfo checkDollarCash(int[] iconArr) {
        if (this.dollarCashConfig == null) {
            return null;
        }

        //出现的美元图标个数
        int dollarIconIdShowCount = 0;
        for (int coloumnId : this.dollarCashConfig.getDollarIconIdShowColumn()) {
            int index = (coloumnId - 1) * this.baseInitCfg.getRows() + 1;
            for (int i = 0; i < this.baseInitCfg.getRows(); i++) {
                int iconId = iconArr[index];
                if (iconId == this.dollarCashConfig.getDollarIconId()) {
                    dollarIconIdShowCount++;
                }
                index++;
            }
        }

        if (dollarIconIdShowCount < 1) {
            return null;
        }

        log.debug("检查到美元图标个数 dollarIconIdShowCount = {}", dollarIconIdShowCount);

        PropInfo propInfo = this.iconTimesPropMap.get(this.dollarCashConfig.getDollarIconId());

        DollarInfo dollarInfo = new DollarInfo();
        int all = 0;
        //生成美元的倍数
        for (int i = 0; i < dollarIconIdShowCount; i++) {
            int key = propInfo.getRandKey();
            dollarInfo.addDollarTimes(key);
            all += key;
        }

        //出现的保险箱图标个数
        int safeBoxIconIdShowCount = 0;
        for (int coloumnId : this.dollarCashConfig.getSafeBoxIconIdShowColumn()) {
            int index = (coloumnId - 1) * this.baseInitCfg.getRows() + 1;
            for (int i = 0; i < this.baseInitCfg.getRows(); i++) {
                int iconId = iconArr[index];
                if (iconId == this.dollarCashConfig.getSafeBoxId()) {
                    safeBoxIconIdShowCount++;
                }
                index++;
            }
        }
        if (safeBoxIconIdShowCount > 0) {
            dollarInfo.setDollarCashTimes(all);

            log.debug("检查中奖美元现金，总倍数 times = {}", all);
        }
        return dollarInfo;
    }

    /**
     * 触发小游戏
     *
     * @param slotsResultLib
     * @param miniGameId
     * @param rotateState
     * @return
     */
    protected DollarExpressResultLib triggerMiniGame(DollarExpressResultLib slotsResultLib, int miniGameId, int rotateState) throws Exception {
        if (this.auxiliaryAecursionCount > 10) {
            log.error("已经递归触发了10次小游戏，强制跳出  miniGameId = {}", miniGameId);
            return slotsResultLib;
        }
        this.auxiliaryAecursionCount++;
        //根据小游戏id去找相关配置
        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
        if (specialAuxiliaryCfg == null) {
            log.warn("未找到该小游戏的配置 miniGameId = {}", miniGameId);
            return slotsResultLib;
        }

        //随机次数
        PropInfo propInfo = this.specialAuxiliaryRandCountPropMap.get(miniGameId).get(slotsResultLib.getRollerMode());
        if (propInfo == null) {
            log.warn("该小游戏的权重信息未找到 miniGameId = {}", miniGameId);
            return slotsResultLib;
        }
        Integer count = propInfo.getRandKey();
        if (count == null || count < 1) {
            log.warn("获取的随机次数为空或小于0， miniGameId = {},rollerMode = {},count = {}", miniGameId, slotsResultLib.getRollerMode(), count);
            return slotsResultLib;
        }
        log.debug("触发小游戏id = {},rotateState = {},count = {}", miniGameId, rotateState, count);

        //处理固定奖励
        if (specialAuxiliaryCfg.getFreeAward() != null && !specialAuxiliaryCfg.getFreeAward().isEmpty()) {
            for (List<Integer> rewardIds : specialAuxiliaryCfg.getFreeAward()) {
                slotsResultLib = handSpecialAuxiliaryAward(slotsResultLib, rewardIds, specialAuxiliaryCfg, count, rotateState);
            }
        }

        slotsResultLib = handSpecialRandReward(slotsResultLib, specialAuxiliaryCfg, count, rotateState);
        return slotsResultLib;
    }

    /**
     * 处理特殊游戏的随机奖励
     *
     * @param slotsResultLib
     * @param cfg
     * @param count
     * @param rotateState
     * @return
     */
    public DollarExpressResultLib handSpecialRandReward(DollarExpressResultLib slotsResultLib, SpecialAuxiliaryCfg cfg, int count, int rotateState) throws Exception{
        if (cfg.getFreeRandAward() == null || cfg.getFreeRandAward().isEmpty()) {
            return slotsResultLib;
        }
        log.debug("找到随机 specialAuxiliary 的随机奖励 = {}", cfg.getId());

        PropAndAwardInfo<FreeRandAwardInfo> propAndAwardInfo = this.specialAuxiliaryRandAwardPropMap.get(cfg.getId());
        if (propAndAwardInfo == null) {
            log.warn("没有找到该小游戏的 免费随机奖励配置 miniGameId = {}", cfg.getId());
            return slotsResultLib;
        }

        if (cfg.getType() == SlotsConst.SpecialAuxiliary.TYPE_OPEN_BOX) {

        } else if (cfg.getType() == SlotsConst.SpecialAuxiliary.TYPE_FREE_ROLL) {
            List<Integer> idList = new ArrayList<>();
            for (Map.Entry<Integer, FreeRandAwardInfo> freeEn : propAndAwardInfo.getAwardMap().entrySet()) {  //免费旋转
                FreeRandAwardInfo freeRandAwardInfo = freeEn.getValue();
                //检查模式id是否一致
                if (freeRandAwardInfo.getModelId() != slotsResultLib.getRollerMode()) {
                    continue;
                }
                idList.add(freeRandAwardInfo.getAwardId());
            }
            //处理奖励逻辑
            slotsResultLib = handSpecialAuxiliaryAward(slotsResultLib, idList, cfg, count, rotateState);
        }
        return slotsResultLib;
    }

    /**
     * 处理 SpecialAuxiliary 表中的奖励
     *
     * @param slotsResultLib
     * @param rewardIds
     * @param specialAuxiliaryCfg
     * @param count
     * @param rotateState
     * @return
     */
    public DollarExpressResultLib handSpecialAuxiliaryAward(DollarExpressResultLib slotsResultLib, List<Integer> rewardIds, SpecialAuxiliaryCfg specialAuxiliaryCfg, int count, int rotateState) throws Exception{
        log.debug("处理 specialAuxiliartAward 奖励逻辑 specialAuxiliaryId =  {}", rewardIds);

        //根据配置，找到真正的数据
        FreeAwardRealData freeAwardRealData = getFreeAwardRealData(rewardIds, count);

        //奖励A
        if(freeAwardRealData.getResultListA() != null && !freeAwardRealData.getResultListA().isEmpty()) {
            outer:
            for (int[] daraArr : freeAwardRealData.getResultListA()) {
                AuxiliaryAwardType auxiliaryAwardType = AuxiliaryAwardType.getType(daraArr[0]);
                int data = daraArr[1];

                switch (auxiliaryAwardType) {
                    case GOLD_PROP:
                        slotsResultLib = triggerGoldProp(slotsResultLib,count,rotateState);
                        break;
                    case FREE_GAME_COUNT:
                        slotsResultLib = triggerFreeGame(slotsResultLib, freeAwardRealData.getResultListA(), specialAuxiliaryCfg, count, rotateState);
                        break outer;
                    case REWARD_MINI_GAME:
                        slotsResultLib = triggerMiniGame(slotsResultLib, data, rotateState);
                        break;
                    case SPIN_COUNT_AGAIN:
                        slotsResultLib = triggerAgainGame(slotsResultLib, freeAwardRealData.getResultListA(), specialAuxiliaryCfg, count, rotateState);
                        break outer;
                }
            }
        }

        //奖励C
        if(freeAwardRealData.getResultMapC() != null && !freeAwardRealData.getResultMapC().isEmpty()) {
            for(Map.Entry<Integer,List<int[]>> en : freeAwardRealData.getResultMapC().entrySet()){
                List<int[]> rewardCList = en.getValue();

                Train train = new Train();
                train.setSpecialAuxiliaryId(en.getKey());
                for (int[] daraArr : rewardCList) {
                    train.addCoach(daraArr[0],daraArr[1]);
                }

                if(rotateState == SlotsConst.BaseElementReward.ROTATESTATE_FREE){
                    slotsResultLib.addFreeGameTrain(this.currentFreeGameId,train);
                    log.debug("免费添加火车信息 specialAuxiliaryId = {}", train.getSpecialAuxiliaryId());
                }else if(rotateState == SlotsConst.BaseElementReward.ROTATESTATE_AGAIN){
                    slotsResultLib.addAgainGameTrain(this.currentAgainGameId,train);
                    log.debug("重转添加火车信息 specialAuxiliaryId = {}", train.getSpecialAuxiliaryId());
                }else {
                    slotsResultLib.addTrain(train);
                    log.debug("正常添加火车信息 specialAuxiliaryId = {}", train.getSpecialAuxiliaryId());
                }
            }
        }
        return slotsResultLib;
    }

    /**
     * 金币系数
     * @param slotsResultLib
     * @param count
     * @param rotateState
     * @return
     */
    private DollarExpressResultLib triggerGoldProp(DollarExpressResultLib slotsResultLib, int count, int rotateState){
        if(rotateState == SlotsConst.BaseElementReward.ROTATESTATE_FREE){
            slotsResultLib.addFreeGameGoldTrainCount(this.currentFreeGameId,count);
        }else if(rotateState == SlotsConst.BaseElementReward.ROTATESTATE_AGAIN){
            slotsResultLib.addAgainGameGoldTrainCount(this.currentAgainGameId,count);
        }else {
            slotsResultLib.setGoldTrainCount(count);
        }
        return slotsResultLib;
    }


    /**
     * 触发免费转
     *
     * @param slotsResultLib
     * @param specialAuxiliaryCfg
     * @param count
     * @param rotateState
     * @return
     */
    private DollarExpressResultLib triggerFreeGame(DollarExpressResultLib slotsResultLib, List<int[]> specialAuxiliaryAwardDataList, SpecialAuxiliaryCfg specialAuxiliaryCfg, int count, int rotateState) throws Exception{
        int[] arr1 = specialAuxiliaryAwardDataList.get(0);
        int[] arr2 = specialAuxiliaryAwardDataList.get(1);

        int freeCount = 0;
        int appointRoller = 0;

        if (arr1[0] == AuxiliaryAwardType.FREE_GAME_COUNT.getType() && arr2[0] == AuxiliaryAwardType.APPOINT_ROLLER.getType()) {
            freeCount = arr1[1];
            appointRoller = arr2[1];
        } else if (arr1[0] == AuxiliaryAwardType.APPOINT_ROLLER.getType() && arr2[0] == AuxiliaryAwardType.FREE_GAME_COUNT.getType()) {
            freeCount = arr2[1];
            appointRoller = arr1[1];
        } else {
            log.warn("解析 免费转 数据错误 ");
            return slotsResultLib;
        }

        DollarExpressResultLib tempLib = null;

        if(rotateState == SlotsConst.BaseElementReward.ROTATESTATE_NORMAL){
            tempLib = slotsResultLib.copyBaseData();
            tempLib.setRollerId(appointRoller);
            this.branchLibMap.put(SlotsConst.Common.TYPE_FREE, tempLib);
        }else {
            tempLib = slotsResultLib;
        }

        log.debug("触发免费游戏 count = {},appointRoller = {},rotateState = {}", freeCount,appointRoller,rotateState);
        for (int i = 0; i < freeCount; i++) {
            //生成20个图标
            int[] arr = generateAllIcons(appointRoller);
            if (arr == null) {
                continue;
            }
            log.debug("开始免费游戏的第 {} 次",i);
            this.currentFreeGameId = i;
            //格子修改
            girdUpdate(appointRoller,specialAuxiliaryCfg.getSpinType(),SlotsConst.BaseElementReward.ROTATESTATE_FREE,arr);
            //检查普通奖励
            List<DollarExpressAwardLineInfo> awardLineInfoList = normalAward(arr, SlotsConst.BaseElementReward.ROTATESTATE_FREE);
            tempLib.addFreeGame(i, arr, awardLineInfoList);

            //检查特殊奖励
            tempLib = specialAward(arr, tempLib, SlotsConst.BaseElementReward.ROTATESTATE_FREE);
        }
        return slotsResultLib;
    }

    /**
     * 触发重转
     *
     * @param slotsResultLib
     * @param specialAuxiliaryCfg
     * @param count
     * @param rotateState
     * @return
     */
    private DollarExpressResultLib triggerAgainGame(DollarExpressResultLib slotsResultLib, List<int[]> specialAuxiliaryAwardDataList, SpecialAuxiliaryCfg specialAuxiliaryCfg, int count, int rotateState) throws Exception {
        int[] arr1 = specialAuxiliaryAwardDataList.get(0);
        int[] arr2 = specialAuxiliaryAwardDataList.get(1);

        int againCount = 0;
        int appointRoller = 0;

        if (arr1[0] == AuxiliaryAwardType.SPIN_COUNT_AGAIN.getType() && arr2[0] == AuxiliaryAwardType.APPOINT_ROLLER.getType()) {
            againCount = arr1[1];
            appointRoller = arr2[1];
        } else if (arr1[0] == AuxiliaryAwardType.APPOINT_ROLLER.getType() && arr2[0] == AuxiliaryAwardType.SPIN_COUNT_AGAIN.getType()) {
            againCount = arr2[1];
            appointRoller = arr1[1];
        } else {
            log.warn("解析 重转 数据错误 ");
            return slotsResultLib;
        }

        DollarExpressResultLib tempLib = null;

        if(rotateState == SlotsConst.BaseElementReward.ROTATESTATE_NORMAL){
            tempLib = slotsResultLib.copyBaseData();
            tempLib.setRollerId(appointRoller);
            this.branchLibMap.put(SlotsConst.Common.TYPE_TRAIN, tempLib);
        }else {
            tempLib = slotsResultLib;
        }

        log.debug("触发重转游戏 count = {},appointRoller = {},rotateState = {}", againCount,appointRoller,rotateState);
        for (int i = 0; i < againCount; i++) {
            //生成20个图标
            int[] arr = generateAllIcons(appointRoller);
            if (arr == null) {
                continue;
            }
            this.currentAgainGameId = i;

            tempLib.initAgainGame(i,arr);
            log.debug("开始重转游戏的第 {} 次",i);
            //格子修改
            girdUpdate(appointRoller,specialAuxiliaryCfg.getSpinType(),SlotsConst.BaseElementReward.ROTATESTATE_AGAIN,arr);

            //检查普通奖励
//            List<DollarExpressAwardLineInfo> awardLineInfoList = normalAward(arr, SlotsConst.BaseElementReward.ROTATESTATE_AGAIN);
//            tempLib.addFreeGame(i, arr, awardLineInfoList);

            //检查特殊奖励
            tempLib = specialAward(arr, tempLib, SlotsConst.BaseElementReward.ROTATESTATE_AGAIN);
        }
        return slotsResultLib;
    }

    /**********************************************************************************************/

    /**
     * 根据iconid判断是不是火车小游戏
     *
     * @return
     */
    private boolean isNormalTrainByIconId(int iconId) {
        return iconId == DollarExpressConstant.BaseElement.ID_GREEN_TRAIN ||
                iconId == DollarExpressConstant.BaseElement.ID_BLUE_TRAIN ||
                iconId == DollarExpressConstant.BaseElement.ID_PURPLE_TRAIN ||
                iconId == DollarExpressConstant.BaseElement.ID_RED_TRAIN;
    }

    /**
     * 根据iconid判断黄金列车
     *
     * @return
     */
    private boolean isGoldTrainByIconId(int iconId) {
        return iconId == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN;
    }

    private void specialPlayConfig() {
        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            String value = cfg.getValue();
            String[] arr = value.split(";");

            if (cfg.getPlayType() == DollarExpressConstant.SpecialPlay.TYPE_RAND_TIMES) {
                this.iconTimesPropMap = new HashMap<>();

                for (String str : arr) {
                    String[] arr2 = str.split(",");

                    PropInfo propInfo = this.iconTimesPropMap.computeIfAbsent(Integer.parseInt(arr2[0]), k -> new PropInfo());

                    String[] arr3 = arr2[1].split("-");
                    int begin = 0;
                    int end = 0;
                    for (String s : arr3) {
                        begin = end;

                        String[] arr4 = s.split("&");
                        end += Integer.parseInt(arr4[1]);
                        propInfo.addProp(Integer.parseInt(arr4[0]), begin, end);
                    }
                    propInfo.setSum(end);
                }


            } else if (cfg.getPlayType() == DollarExpressConstant.SpecialPlay.TYPE_DOLLAR_CASH) {
                this.dollarCashConfig = new DollarCashConfig();

                String[] arr2 = arr[0].split(",");
                this.dollarCashConfig.setDollarIconId(Integer.parseInt(arr2[0]));
                String[] arr3 = arr2[1].split("&");
                for (String s : arr3) {
                    this.dollarCashConfig.addDollarIconIdShowColumn(Integer.parseInt(s));
                }

                String[] arr4 = arr[1].split(",");
                this.dollarCashConfig.setSafeBoxId(Integer.parseInt(arr4[0]));
                this.dollarCashConfig.addSafeBoxIconIdShowColumn(Integer.parseInt(arr4[1]));

                this.dollarCashConfig.setCollectIconId(Integer.parseInt(arr[3]));
            }
        }
    }

    @Override
    protected boolean girdSpecialElement(int iconId) {
        return iconId == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN || iconId == DollarExpressConstant.BaseElement.ID_SAFE_BOX;
    }

    /**
     * 计算倍数
     * @param lib
     */
    private void calTimes(DollarExpressResultLib lib){
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //火车
        int trainTimes = calTrainTimes(lib.getTrainList());
        if(trainTimes > 0){
            lib.addTimes(0);
            lib.setLibType(SlotsConst.SpecialResultLib.TYPE_TRAIN);
        }


        //免费游戏
        if(lib.getFreeGameMap() != null && !lib.getFreeGameMap().isEmpty()){
            lib.setLibType(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD);
            for(Map.Entry<Integer, DollarExpressFreeGame> en : lib.getFreeGameMap().entrySet()){
                DollarExpressFreeGame game = en.getValue();
                //中奖线
                game.addTimes(calLineTimes(game.getAwardLineInfoList()));
                //火车
                game.addTimes(calTrainTimes(game.getTrainList()));
                //美元现金
                game.addTimes(calDollarCashTimes(game.getDollarCashInfo()));
                //黄金列车
                game.addTimes(calGoldTrainTimes(game.getDollarCashInfo() == null ? null : game.getDollarCashInfo().getDollarTimesList(), game.getGoldTrainCount()));
                //添加到总倍数
                lib.addTimes(game.getTimes());
            }
        }

        //重转
        if(lib.getAgainGameMap() != null && !lib.getAgainGameMap().isEmpty()){
            lib.setLibType(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD);
            for(Map.Entry<Integer, DollarExpressAgainGame> en : lib.getAgainGameMap().entrySet()){
                DollarExpressAgainGame game = en.getValue();
                //火车
                game.addTimes(calTrainTimes(game.getTrainList()));
                //黄金列车
                game.addTimes(calGoldTrainTimes(game.getDollarTimesList(), game.getGoldTrainCount()));
                //添加到总倍数
                lib.addTimes(game.getTimes());
            }
        }

        //美元现金
        lib.addTimes(calDollarCashTimes(lib.getDollarCashInfo()));
        //黄金列车
        int goldTrainTimes = calGoldTrainTimes(lib.getDollarCashInfo() == null ? null : lib.getDollarCashInfo().getDollarTimesList(), lib.getGoldTrainCount());
        if(goldTrainTimes > 0){
            lib.addTimes(goldTrainTimes);
            lib.setLibType(SlotsConst.SpecialResultLib.TYPE_GOLD_TRAIN);
        }
    }

    /**
     * 计算中奖线的倍数
     * @param list
     * @return
     */
    private int calLineTimes(List<DollarExpressAwardLineInfo> list){
        int times = 0;
        if(list != null && !list.isEmpty()){
            for(DollarExpressAwardLineInfo awardLineInfo : list){
                times += awardLineInfo.getBaseTimes();
                if(awardLineInfo.getOtherIconAwardInfoMap() != null && !awardLineInfo.getOtherIconAwardInfoMap().isEmpty()){
                    for(Map.Entry<Integer, Integer> en : awardLineInfo.getOtherIconAwardInfoMap().entrySet()){
                        times += en.getValue();
                    }
                }
            }
        }
        return times;
    }

    /**
     * 计算中奖线的倍数
     * @param list
     * @return
     */
    private int calTrainTimes(List<Train> list){
        int times = 0;
        if(list != null && !list.isEmpty()){
            for(Train train : list){
                if(train.getCoachs() != null && !train.getCoachs().isEmpty()){
                    for(int[] arr : train.getCoachs()){
                        times += arr[1];
                    }
                }
            }
        }
        return times;
    }

    /**
     * 美元现金
     * @param dollarInfo
     * @return
     */
    private int calDollarCashTimes(DollarInfo dollarInfo){
        if(dollarInfo == null){
            return 0;
        }

        return dollarInfo.getDollarCashTimes();
    }

    /**
     * 黄金列车
     * @return
     */
    private int calGoldTrainTimes(List<Integer> list,int count){
        if(list == null || list.isEmpty() || count < 1){
            return 0;
        }
        int times = 0;
        for(int t : list){
            times += t;
        }
        return times * count;
    }

}
