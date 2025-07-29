package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.slots.constant.AuxiliaryAwardType;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.*;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.*;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * @author 11
 * @date 2025/7/7 18:28
 */
@Component
public class DollarExpressGenerateManager extends AbstractSlotsGenerateManager<DollarExpressAwardLineInfo,DollarExpressResultLib> {
    public DollarExpressGenerateManager() {
        super(DollarExpressResultLib.class);
    }

    private DollarCashConfig dollarCashConfig;
    private DollarExpressCollectDollarConfig dollarExpressCollectDollarConfig;
    //随机倍数
    private Map<Integer, PropInfo> iconTimesPropMap;

    //从specialPlay中读取playType = 5时，获得的小游戏id
    private int goldTrainAuxiliaryId = 0;

    @Override
    public void initConfig() {
        super.initConfig();

        specialPlayConfig();
    }

    /**
     * 添加中奖线信息
     * @param baseLineCfg
     * @param rewardCfg
     * @param sameCount
     * @param baseIconId
     * @param lineList
     * @param arr
     * @return
     */
    @Override
    protected DollarExpressAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg,BaseElementRewardCfg rewardCfg,int sameCount,
                                                          int baseIconId,List<Integer> lineList,int[] arr) {
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

    @Override
    public DollarExpressResultLib specialAward(int[] iconArr, DollarExpressResultLib lib, int rotateState, List<DollarExpressResultLib> libList, int index) throws Exception {
        // 统计arr中每个元素的出现次数
        Map<Integer, Integer> iconShowCountMap = iconShowCount(iconArr);

        //检查baseLineFree配置的出现的特殊元素，  线路玩法 -> 主元素id -> 出现总次数
        Map<Integer, Map<Integer, Integer>> showIdMap = baseLineFreeShowId(iconShowCountMap);

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
                //匹配连线的元素id和个数
                Integer showCount = null;

                //检查tempShowIdMap是否有特殊元素的个数
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
                Integer miniGameId = featureTriggerIdMap.get(lib.getRollerMode());
                if (miniGameId == null || miniGameId < 1) {
                    continue;
                }

                //出现的元素种类数
                if(!checkIconTypes(iconId,iconShowCountMap)){
                    continue;
                }

                if(trainId(iconId)){
                    int forCount = showCount - 1;
                    for(int i=0;i<forCount;i++){
                        lib = triggerMiniGame(lib, iconId, miniGameId, rotateState,libList,index);
                    }
                }else {
                    lib = triggerMiniGame(lib, iconId, miniGameId, rotateState,libList,index);
                }
            }
        }

        return lib;
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
        int safeBoxIconIdShowCount = checkSafeBoxCount(iconArr);
        if (safeBoxIconIdShowCount > 0) {
            dollarInfo.setDollarCashTimes(all);

            log.debug("检查中奖美元现金，总倍数 times = {}", all);
        }
        return dollarInfo;
    }

    /**
     * 检查保险箱图标个数
     * @param arr
     * @return  坐标
     */
    public int checkSafeBoxCount(int[] arr){
        for (int coloumnId : this.dollarCashConfig.getSafeBoxIconIdShowColumn()) {
            int index = (coloumnId - 1) * this.baseInitCfg.getRows() + 1;
            for (int i = 0; i < this.baseInitCfg.getRows(); i++) {
                int iconId = arr[index];
                if (iconId == this.dollarCashConfig.getSafeBoxId()) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    /**
     * 检查黄金列车
     *
     * @param iconArr
     * @return
     */
    public List<int[]> checkGoldTrain(int count,int[] iconArr,int coachs) {
        if (this.dollarCashConfig == null) {
            return null;
        }

        boolean goldTrain = false;
        int index = (this.baseInitCfg.getCols() - 1) * this.baseInitCfg.getRows() + 1;
        for (int i = 0; i < this.baseInitCfg.getRows(); i++) {
            int iconId = iconArr[index+i];
            if (iconId == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN) {
                goldTrain = true;
                break;
            }
        }

        if (!goldTrain) {
            return null;
        }

        //黄金列车触发局美金的总倍数
        PropInfo propInfo = this.iconTimesPropMap.get(DollarExpressConstant.BaseElement.ID_GOLD_TRAIN);
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int[] arr = new int[2];
            int dollarAllTimes = propInfo.getRandKey();

            arr[0] = dollarAllTimes;
            arr[1] = coachs;
            list.add(arr);
        }
        return list;
    }

    /**
     * 处理奖励C
     * @param lib
     * @param freeAwardRealData
     * @param iconId
     * @param count
     * @param rotateState
     * @param specialAuxiliaryCfg
     * @param libList
     * @param index
     * @return
     */
    @Override
    protected DollarExpressResultLib handAwardC(DollarExpressResultLib lib, FreeAwardRealData freeAwardRealData, int iconId, int count, int rotateState, SpecialAuxiliaryCfg specialAuxiliaryCfg, List<DollarExpressResultLib> libList, int index) {
        //奖励C
        if (freeAwardRealData.getResultMapC() != null && !freeAwardRealData.getResultMapC().isEmpty()) {
            for (Map.Entry<Integer, List<int[]>> en : freeAwardRealData.getResultMapC().entrySet()) {
                List<int[]> rewardCList = en.getValue();

                Train train = new Train();
                train.setTrainIconId(iconId);
                for (int[] daraArr : rewardCList) {
                    train.addCoach(daraArr[0], daraArr[1]);
                }

                if (rotateState == SlotsConst.BaseElementReward.ROTATESTATE_FREE) {
                    lib.addFreeGameTrain(index, train);
                    log.debug("免费添加火车信息 trainIconId = {}", iconId);
                } else if (rotateState == SlotsConst.BaseElementReward.ROTATESTATE_AGAIN) {
                    lib.addAgainGameTrain(index, train);
                    log.debug("重转添加火车信息 trainIconId = {}", iconId);
                } else {
                    lib.addTrain(train);
                    log.debug("正常添加火车信息 trainIconId = {}", iconId);
                }
            }
        }
        return lib;
    }

    /**
     * 处理投资游戏中选中3个地图开奖
     * @param idList
     * @return
     */
    public InversGoldTrainRewardData handInversGoldTrainReward(List<Integer> idList){
        InversGoldTrainRewardData data = new InversGoldTrainRewardData();

        for(int rewardId : idList){
            SpecialAuxiliaryAwardCfg specialAuxiliaryAwardCfg = GameDataManager.getSpecialAuxiliaryAwardCfg(rewardId);
            if(specialAuxiliaryAwardCfg == null){
                log.debug("未找到该奖励配置 rewardId = {}", rewardId);
                continue;
            }
            AuxiliaryAwardType auxiliaryAwardType = AuxiliaryAwardType.getType(specialAuxiliaryAwardCfg.getType());
            if(auxiliaryAwardType == AuxiliaryAwardType.GOLD){
                PropAndAwardInfo<SpecialAuxiliaryAwardC> specialAuxiliaryAwardCPropAndAwardInfo = this.specialAuxiliaryAwardInfoMapC.get(specialAuxiliaryAwardCfg.getId());
                PropInfo propInfo = specialAuxiliaryAwardCPropAndAwardInfo.getPropInfo(specialAuxiliaryAwardCfg.getId());
                Integer key = propInfo.getRandKey();
                if (key == null) {
                    log.warn("randAwardC 随机获取key为空 cfg.id = {}", specialAuxiliaryAwardCfg.getId());
                    continue;
                }

                SpecialAuxiliaryAwardC awardCInfo = specialAuxiliaryAwardCPropAndAwardInfo.getAwardInfo(key);
                data.addTimes(awardCInfo.getTimes());

            }else if(auxiliaryAwardType == AuxiliaryAwardType.GOLD_PROP){
                PropInfo propInfo = this.specialAuxiliaryAwardInfoMapA.get(specialAuxiliaryAwardCfg.getId());

                Integer key = propInfo.getRandKey();
                if (key == null) {
                    log.warn("randAwardA 随机获取key为空 cfg.id = {}", specialAuxiliaryAwardCfg.getId());
                    continue;
                }
                data.setGoldTrain(key);
            }
        }
        return data;
    }

    /**
     * 金币系数
     *
     * @param slotsResultLib
     * @param count
     * @param rotateState
     * @return
     */
    protected DollarExpressResultLib triggerGoldProp(DollarExpressResultLib slotsResultLib, int count, int rotateState,int awardData,int index) {
        if (rotateState == SlotsConst.BaseElementReward.ROTATESTATE_FREE) {
            int[] iconArr = slotsResultLib.getFreeGameMap().get(index).getIconArr();
            slotsResultLib.addFreeGameGoldTrainCount(index,checkGoldTrain(count,  iconArr,awardData));
        } else if (rotateState == SlotsConst.BaseElementReward.ROTATESTATE_AGAIN) {
            int[] iconArr = slotsResultLib.getAgainGameMap().get(index).getIconArr();
            slotsResultLib.addAgainGameGoldTrainCount(index,checkGoldTrain(count,  iconArr,awardData));
        } else {
            List<int[]> list = checkGoldTrain(count, slotsResultLib.getIconArr(),awardData);
            if(list != null && !list.isEmpty()){
                int[] arr = list.get(0);
                slotsResultLib.setGoldTrainCount(arr[1]);
                slotsResultLib.setGoldTrainAllTimes(arr[0]);
            }
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
    protected DollarExpressResultLib triggerFreeGame(DollarExpressResultLib slotsResultLib,
                                                   List<int[]> specialAuxiliaryAwardDataList,
                                                   SpecialAuxiliaryCfg specialAuxiliaryCfg,
                                                   int count,
                                                   int rotateState,
                                                   List<DollarExpressResultLib> libList) throws Exception {
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

        if (rotateState == SlotsConst.BaseElementReward.ROTATESTATE_NORMAL) {
            tempLib = slotsResultLib.copyBaseData();
            tempLib.setRollerId(appointRoller);
            tempLib.setLibType(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD_FREE);
            libList.add(tempLib);
            slotsResultLib.setLibType(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD);
        } else {
            tempLib = slotsResultLib;
        }

        log.debug("触发免费游戏 count = {},appointRoller = {},rotateState = {}", freeCount, appointRoller, rotateState);
        for (int i = 0; i < freeCount; i++) {
            //生成20个图标
            int[] arr = generateAllIcons(appointRoller);
            if (arr == null) {
                continue;
            }
            log.debug("开始免费游戏的第 {} 次", i);
            //格子修改
            girdUpdate(appointRoller, specialAuxiliaryCfg.getSpinType(), SlotsConst.BaseElementReward.ROTATESTATE_FREE, arr);
            //检查普通奖励
            List<DollarExpressAwardLineInfo> awardLineInfoList = normalAward(arr, SlotsConst.BaseElementReward.ROTATESTATE_FREE);
            tempLib.addFreeGame(i, arr, awardLineInfoList);

            //检查特殊奖励
            tempLib = specialAward(arr, tempLib, SlotsConst.BaseElementReward.ROTATESTATE_FREE,libList,i);
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
    protected DollarExpressResultLib triggerAgainGame(DollarExpressResultLib slotsResultLib,
                                                    List<int[]> specialAuxiliaryAwardDataList,
                                                    SpecialAuxiliaryCfg specialAuxiliaryCfg,
                                                    int count,
                                                    int rotateState,
                                                    List<DollarExpressResultLib> libList) throws Exception {
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

        if (rotateState == SlotsConst.BaseElementReward.ROTATESTATE_NORMAL) {
            tempLib = slotsResultLib.copyBaseData();
            tempLib.setRollerId(appointRoller);
            tempLib.setLibType(SlotsConst.SpecialResultLib.TYPE_TRAIN);
            slotsResultLib.setLibType(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD);
            libList.add(tempLib);
        } else {
            tempLib = slotsResultLib;
        }

        log.debug("触发重转游戏 count = {},appointRoller = {},rotateState = {}", againCount, appointRoller, rotateState);
        for (int i = 0; i < againCount; i++) {
            //生成20个图标
            int[] arr = generateAllIcons(appointRoller);
            if (arr == null) {
                continue;
            }

            tempLib.initAgainGame(i, arr);
            log.debug("开始重转游戏的第 {} 次", i);
            //格子修改
            girdUpdate(appointRoller, specialAuxiliaryCfg.getSpinType(), SlotsConst.BaseElementReward.ROTATESTATE_AGAIN, arr);

            //检查特殊奖励
            tempLib = specialAward(arr, tempLib, SlotsConst.BaseElementReward.ROTATESTATE_AGAIN,libList,i);
        }
        return slotsResultLib;
    }

    /**
     * 是否为火车id
     * @param iconId
     * @return
     */
    private boolean trainId(int iconId){
        if(iconId == DollarExpressConstant.BaseElement.ID_GREEN_TRAIN ||
                iconId == DollarExpressConstant.BaseElement.ID_RED_TRAIN ||
                iconId == DollarExpressConstant.BaseElement.ID_BLUE_TRAIN ||
                iconId == DollarExpressConstant.BaseElement.ID_PURPLE_TRAIN){
            return true;
        }
        return false;
    }

    /**********************************************************************************************/

    private void specialPlayConfig() {
        Map<Integer,PropInfo> map = new HashMap<>();

        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            String value = cfg.getValue();
            String[] arr = value.split(";");

            if (cfg.getPlayType() == DollarExpressConstant.SpecialPlay.TYPE_RAND_TIMES) {
                for (String str : arr) {
                    String[] arr2 = str.split(",");

                    PropInfo propInfo = map.computeIfAbsent(Integer.parseInt(arr2[0]), k -> new PropInfo());

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
            } else if(cfg.getPlayType() == DollarExpressConstant.SpecialPlay.TYPE_GOLD_TRAIN){
                this.goldTrainAuxiliaryId = Integer.parseInt(arr[1]);
                String[] arr2 = arr[0].split(",");

                PropInfo propInfo = map.computeIfAbsent(Integer.parseInt(arr2[0]), k -> new PropInfo());

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
            }else if (cfg.getPlayType() == DollarExpressConstant.SpecialPlay.TYPE_DOLLAR_CASH) {
                DollarCashConfig config = new DollarCashConfig();

                String[] arr2 = arr[0].split(",");
                config.setDollarIconId(Integer.parseInt(arr2[0]));
                String[] arr3 = arr2[1].split("&");
                for (String s : arr3) {
                    config.addDollarIconIdShowColumn(Integer.parseInt(s));
                }

                String[] arr4 = arr[1].split(",");
                config.setSafeBoxId(Integer.parseInt(arr4[0]));
                config.addSafeBoxIconIdShowColumn(Integer.parseInt(arr4[1]));

                config.setCollectIconId(Integer.parseInt(arr[3]));

                this.dollarCashConfig = config;
            }else if(cfg.getPlayType() == DollarExpressConstant.SpecialPlay.TYPE_INVERS){
                DollarExpressCollectDollarConfig config = new DollarExpressCollectDollarConfig();

                String[] arr2 = arr[0].split(",");
                config.setStakeMin(Long.parseLong(arr2[2]));
                config.setBegin(Integer.parseInt(arr2[3]));
                config.setProp(Integer.parseInt(arr2[4]));
                config.setMax(Integer.parseInt(arr2[5]));
                config.setAuxiliaryId(Integer.parseInt(arr2[6]));

                this.dollarExpressCollectDollarConfig = config;
            }
        }

        if(!map.isEmpty()){
            this.iconTimesPropMap = map;
        }
    }

    /**
     * 检查是不是特殊元素
     * @param iconId
     * @return
     */
    @Override
    protected boolean girdSpecialElement(int iconId) {
        return iconId == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN || iconId == DollarExpressConstant.BaseElement.ID_SAFE_BOX;
    }

    /**
     * 计算倍数
     *
     * @param lib
     */
    public void calTimes(DollarExpressResultLib lib) throws Exception{
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //火车
        CommonResult<Integer> trainResult = calTrainTimes(lib.getTrainList());
        if (trainResult.success()) {
            lib.addTimes(trainResult.data);
            lib.setLibType(SlotsConst.SpecialResultLib.TYPE_TRAIN);
        }

        if (lib.getGoldTrainCount() > 0) {
//            lib.addTimes(goldTrainTimes);
            lib.setLibType(SlotsConst.SpecialResultLib.TYPE_GOLD_TRAIN);
        }

        //免费游戏
        if (lib.getFreeGameMap() != null && !lib.getFreeGameMap().isEmpty()) {
//            lib.setLibType(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD_FREE);
            for (Map.Entry<Integer, DollarExpressFreeGame> en : lib.getFreeGameMap().entrySet()) {
                DollarExpressFreeGame game = en.getValue();
                //中奖线
                game.addTimes(calLineTimes(game.getAwardLineInfoList()));
                //火车
                trainResult = calTrainTimes(game.getTrainList());
                if (trainResult.success()) {
                    game.addTimes(trainResult.data);
                }
                //美元现金
                game.addTimes(calDollarCashTimes(game.getDollarInfo()));
                //黄金列车
//                game.addTimes(calGoldTrainTimes(game.getDollarInfo() == null ? null : game.getDollarInfo().getDollarTimesList(), game.getGoldTrainCount()));
//                if(game.getGoldTrainCount() > 0){
//                    lib.setLibType(SlotsConst.SpecialResultLib.TYPE_GOLD_TRAIN);
//                }
                //添加到总倍数
                lib.addTimes(game.getTimes());
            }
        }

        //重转
        if (lib.getAgainGameMap() != null && !lib.getAgainGameMap().isEmpty()) {
            Iterator<Map.Entry<Integer, DollarExpressAgainGame>> it = lib.getAgainGameMap().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, DollarExpressAgainGame> en = it.next();
                DollarExpressAgainGame game = en.getValue();

                //火车
                trainResult = calTrainTimes(game.getTrainList());
                if (trainResult.success()) {
                    game.addTimes(trainResult.data);
                    lib.setLibType(SlotsConst.SpecialResultLib.TYPE_AGAIN_TRAIN);

                    if(lib.getIconArr() != null && lib.getIconArr().length > 0){
                        throw new IllegalArgumentException("重转1的lib里面不会有iconArr");
                    }

                    lib.setIconArr(game.getIconArr());
                    lib.setTrainList(game.getTrainList());
                    it.remove();
                }

                if(game.getGoldTrainCount() > 0){
                    lib.setLibType(SlotsConst.SpecialResultLib.TYPE_AGAIN_GOLD_TRAIN);
                    if(lib.getIconArr() != null && lib.getIconArr().length > 0){
                        throw new IllegalArgumentException("重转2的lib里面不会有iconArr");
                    }

                    lib.setIconArr(game.getIconArr());
                    lib.setGoldTrainCount(game.getGoldTrainCount());
                    lib.setGoldTrainAllTimes(game.getGoldTrainAllTimes());
                    it.remove();
                }

                if(trainResult.success() && game.getGoldTrainCount() > 0){
                    log.warn("普通火车和黄金列车同时出现了....");
                    throw new IllegalArgumentException("普通火车和黄金列车同时出现了....");
                }

                //添加到总倍数
                lib.addTimes(game.getTimes());
            }

            if(lib.getAgainGameMap().isEmpty()){
                lib.setAgainGameMap(null);
            }
        }

        //美元现金
        lib.addTimes(calDollarCashTimes(lib.getDollarInfo()));
        //黄金列车
//        int goldTrainTimes = calGoldTrainTimes(lib.getDollarInfo() == null ? null : lib.getDollarInfo().getDollarTimesList(), lib.getGoldTrainCount());


        if(lib.getRollerId() == SlotsConst.BaseElementReward.ROTATESTATE_FREE){
            lib.setLibType(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD_FREE);
        }
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    private int calLineTimes(List<DollarExpressAwardLineInfo> list) {
        if(list == null || list.isEmpty()){
            return 0;
        }

        int times = 0;
        for (DollarExpressAwardLineInfo awardLineInfo : list) {
            if(awardLineInfo.getOtherIconAwardInfoMap() == null || awardLineInfo.getOtherIconAwardInfoMap().isEmpty()){
                times += awardLineInfo.getBaseTimes();
                continue;
            }
            for (Map.Entry<Integer, Integer> en : awardLineInfo.getOtherIconAwardInfoMap().entrySet()) {
                times += awardLineInfo.getBaseTimes() * en.getValue();
            }
        }
        return times;
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    private CommonResult<Integer> calTrainTimes(List<Train> list) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        int times = 0;
        if (list != null && !list.isEmpty()) {
            for (Train train : list) {
                if (train.getCoachs() != null && !train.getCoachs().isEmpty()) {
                    for (int[] arr : train.getCoachs()) {
                        if(arr[0] < 1){
                            train.setPoolId(arr[1]);
                        }else {
                            times += arr[1];
                        }
                    }
                }
            }
            result.data = times;
        }else {
            result.code = Code.FAIL;
        }
        return result;
    }

    /**
     * 美元现金
     *
     * @param dollarInfo
     * @return
     */
    private int calDollarCashTimes(DollarInfo dollarInfo) {
        if (dollarInfo == null) {
            return 0;
        }

        return dollarInfo.getDollarCashTimes();
    }

    /**
     * 黄金列车
     *
     * @return
     */
    private int calGoldTrainTimes(List<Integer> list, int count) {
        if (list == null || list.isEmpty() || count < 1) {
            return 0;
        }
        int times = 0;
        for (int t : list) {
            times += t;
        }
        return times * count;
    }

    /**
     * 随机一个美元钞票的倍数
     * @return
     */
    public int randDollarTimes(){
        return this.iconTimesPropMap.get(this.dollarCashConfig.getDollarIconId()).getRandKey();
    }


    /**
     * 检查all board 个数
     * @param arr
     * @return
     */
    public int checkAllBoadrd(int[] arr){
        int count = 0;
        for(int i=1;i<arr.length;i++){
            int icon = arr[i];
            if(icon == DollarExpressConstant.BaseElement.ID_ALL_ABOARD){
                count++;
            }
        }
        return count;
    }


    public DollarExpressCollectDollarConfig getDollarExpressCollectDollarConfig() {
        return dollarExpressCollectDollarConfig;
    }

    /**
     * 获取黄金列车节数
     * @return
     */
    public int getGoldTrainCount(){
        if(this.goldTrainAuxiliaryId < 1){
            return 0;
        }

        SpecialAuxiliaryCfg cfg = GameDataManager.getSpecialAuxiliaryCfg(this.goldTrainAuxiliaryId);
        FreeAwardRealData freeAwardRealData = getFreeAwardRealData(cfg.getFreeAward().get(0), 1);
        return freeAwardRealData.getResultListA().get(0)[1];
    }

    @Override
    public void changeSampleCallbackCollector() {
        super.changeSampleCallbackCollector();
        addChangeSampleFileObserveWithCallBack(SpecialPlayCfg.EXCEL_NAME, this::specialPlayConfig);
    }
}
