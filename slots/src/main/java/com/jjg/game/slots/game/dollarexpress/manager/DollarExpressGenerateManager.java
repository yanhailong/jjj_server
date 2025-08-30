package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.AuxiliaryAwardType;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.*;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private DollarCashConfig dollarCashConfig;
    private DollarExpressCollectDollarConfig dollarExpressCollectDollarConfig;
    //随机倍数
    private Map<Integer, PropInfo> iconTimesPropMap;

    //从specialPlay中读取playType = 5时，获得的小游戏id
    private int goldTrainAuxiliaryId = 0;

    //倍数放大了100倍
    private int timesScale = 100;
    private BigDecimal timesScaleBigDecimal = BigDecimal.valueOf(timesScale);

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

    /**********************************************************************************************/

    @Override
    public void generateAfter(DollarExpressResultLib lib) throws Exception {
        //计算倍数
        calTimes(lib);
    }

    /**
     * 计算倍数
     *
     * @param lib
     */
    public void calTimes(DollarExpressResultLib lib) throws Exception {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        //获取单线押分
        BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(1001001);
        int oneLineStake = baseRoomCfg.getLineBetScore().get(0);
        //总押分
        int allLinesStake = baseInitCfg.getMaxLine() * oneLineStake * baseInitCfg.getBetMultiple().get(0) * baseInitCfg.getLineMultiple().get(0);

        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList(),oneLineStake,allLinesStake));
        //火车
        CommonResult<Long> trainResult = calTrainTimes(lib.getTrainList(),oneLineStake,allLinesStake);
        if (trainResult.success()) {
            lib.addTimes(trainResult.data);
            lib.addLibType(SlotsConst.SpecialResultLib.TYPE_TRAIN);

            //火车排序
            lib.setTrainList(sortTrain(lib.getIconArr(),lib.getTrainList()));
        }

        if (lib.getGoldTrainCount() > 0) {
//            lib.addTimes(goldTrainTimes);
            lib.addLibType(SlotsConst.SpecialResultLib.TYPE_GOLD_TRAIN);
        }

        if(lib.getGoldTrainAllTimes() > 0){
            lib.setGoldTrainAllTimes((int)oneLineToAllLineTimes(SlotsConst.Common.ALL_LINE,oneLineStake,lib.getGoldTrainAllTimes(),allLinesStake));
        }

        //免费游戏
        if (lib.getFreeGameMap() != null && !lib.getFreeGameMap().isEmpty()) {
//            lib.setLibType(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD_FREE);
            for (Map.Entry<Integer, DollarExpressFreeGame> en : lib.getFreeGameMap().entrySet()) {
                DollarExpressFreeGame game = en.getValue();
                //中奖线
                game.addTimes(calLineTimes(game.getAwardLineInfoList(),oneLineStake,allLinesStake));
                //火车
                trainResult = calTrainTimes(game.getTrainList(),oneLineStake,allLinesStake);
                if (trainResult.success()) {
                    game.addTimes(trainResult.data);
                    //火车排序
                    game.setTrainList(sortTrain(game.getIconArr(),game.getTrainList()));
                }
                //美元现金
//                game.addTimes(calDollarCashTimes(game.getDollarInfo()));
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
                trainResult = calTrainTimes(game.getTrainList(),oneLineStake,allLinesStake);
                if (trainResult.success()) {
                    game.addTimes(trainResult.data);

                    //火车排序
                    game.setTrainList(sortTrain(game.getIconArr(),game.getTrainList()));

                    lib.addLibType(SlotsConst.SpecialResultLib.TYPE_AGAIN_TRAIN);

                    if (lib.getIconArr() != null && lib.getIconArr().length > 0) {
                        throw new IllegalArgumentException("重转1的lib里面不会有iconArr");
                    }

                    lib.setIconArr(game.getIconArr());
                    lib.setTrainList(game.getTrainList());
                    it.remove();
                }

                if (game.getGoldTrainCount() > 0) {
                    lib.addLibType(SlotsConst.SpecialResultLib.TYPE_AGAIN_GOLD_TRAIN);
                    if (lib.getIconArr() != null && lib.getIconArr().length > 0) {
                        throw new IllegalArgumentException("重转2的lib里面不会有iconArr");
                    }

                    lib.setIconArr(game.getIconArr());
                    lib.setGoldTrainCount(game.getGoldTrainCount());
                    lib.setGoldTrainAllTimes((int)oneLineToAllLineTimes(SlotsConst.Common.ALL_LINE,oneLineStake,game.getGoldTrainAllTimes(),allLinesStake));
                    it.remove();
                }

                if (trainResult.success() && game.getGoldTrainCount() > 0) {
                    log.warn("普通火车和黄金列车同时出现了....");
                    throw new IllegalArgumentException("普通火车和黄金列车同时出现了....");
                }

                //添加到总倍数
                lib.addTimes(game.getTimes());
            }

            if (lib.getAgainGameMap().isEmpty()) {
                lib.setAgainGameMap(null);
            }
        }

        if (lib.getRollerId() == SlotsConst.BaseElementReward.ROTATESTATE_FREE) {
            lib.addLibType(SlotsConst.SpecialResultLib.TYPE_ALL_BOARD_FREE);
        }

        if(lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            lib.addLibType(SlotsConst.SpecialResultLib.TYPE_NORMAL);
        }
    }

    /**
     * 火车排序
     */
    private List<Train> sortTrain(int[] arr, List<Train> trainList) {
        if (trainList == null || trainList.size() < 2) {
            return trainList;
        }

        List<Integer> iconShowList = new ArrayList<>();
        for (int i = 1; i < arr.length; i++) {
            int iconId = arr[i];
            if(trainId(iconId)){
                iconShowList.add(iconId);
            }
        }

        // 创建 iconId 到其在 iconShowList 中位置的映射
        Map<Integer, List<Integer>> iconOrderMap = new HashMap<>();
        for (int i = 0; i < iconShowList.size(); i++) {
            int iconId = iconShowList.get(i);
            iconOrderMap.computeIfAbsent(iconId, k -> new ArrayList<>()).add(i);
        }

        Train[] trainArr = new Train[trainList.size()];
        for(Map.Entry<Integer,List<Integer>> en : iconOrderMap.entrySet()){
            int iconId = en.getKey();
            List<Integer> indexList = en.getValue();

            Iterator<Integer> it = indexList.iterator();
            while (it.hasNext()){
                int index = it.next();
                trainArr[index] = getTrain(trainList, iconId);
            }
        }

        return Arrays.asList(trainArr);
    }

    private Train getTrain(List<Train> trainList,int iconId){
        Iterator<Train> it = trainList.iterator();
        while(it.hasNext()){
            Train t = it.next();
            if(t.getTrainIconId() == iconId){
                it.remove();
                return t;
            }
        }
        return null;
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @param oneLineStake 单线押分
     * @param allLinesStake 总押分
     * @return
     */
    private int calLineTimes(List<DollarExpressAwardLineInfo> list,int oneLineStake,int allLinesStake) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (DollarExpressAwardLineInfo awardLineInfo : list) {
            if (awardLineInfo.getOtherIconAwardInfoMap() == null || awardLineInfo.getOtherIconAwardInfoMap().isEmpty()) {
                times += oneLineToAllLineTimes(SlotsConst.Common.ONE_LINE,oneLineStake,awardLineInfo.getBaseTimes(),allLinesStake);
                continue;
            }
            for (Map.Entry<Integer, Integer> en : awardLineInfo.getOtherIconAwardInfoMap().entrySet()) {
                int tmpTimes = awardLineInfo.getBaseTimes() * en.getValue();
                times += oneLineToAllLineTimes(SlotsConst.Common.ONE_LINE,oneLineStake,tmpTimes,allLinesStake);

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
    private CommonResult<Long> calTrainTimes(List<Train> list,int oneLineStake,int allLinesStake) {
        CommonResult<Long> result = new CommonResult<>(Code.SUCCESS);
        long times = 0;
        if (list != null && !list.isEmpty()) {
            for (Train train : list) {
                if (train.getCoachs() != null && !train.getCoachs().isEmpty()) {
                    for (int[] arr : train.getCoachs()) {
                        if (arr[0] < 1) {
                            train.setPoolId(arr[1]);
                        } else {
                            int tmpTimes = (int)oneLineToAllLineTimes(arr[0],oneLineStake,arr[1],allLinesStake);
                            arr[1] = tmpTimes;
                            times += tmpTimes;
                        }
                    }
                }
            }
            result.data = times;
        } else {
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
     *
     * @return
     */
    public int randDollarTimes() {
        return this.iconTimesPropMap.get(this.dollarCashConfig.getDollarIconId()).getRandKey();
    }


    /**
     * 检查all board 个数
     *
     * @param arr
     * @return
     */
    public int checkAllBoadrd(int[] arr) {
        int count = 0;
        for (int i = 1; i < arr.length; i++) {
            int icon = arr[i];
            if (icon == DollarExpressConstant.BaseElement.ID_ALL_ABOARD) {
                count++;
            }
        }
        return count;
    }


    public DollarExpressCollectDollarConfig getDollarExpressCollectDollarConfig() {
        return dollarExpressCollectDollarConfig;
    }

    /**
     * 单线押分倍数，转化为总押分倍数
     * @param oneLineStake
     * @param times
     * @param allLinesStake
     * @return
     */
    public long oneLineToAllLineTimes(int rewardType,int oneLineStake,long times,int allLinesStake){
        if(rewardType == SlotsConst.Common.ONE_LINE){
            BigDecimal step1 = BigDecimal.valueOf(oneLineStake).multiply(BigDecimal.valueOf(times));
            BigDecimal step2 = step1.divide(BigDecimal.valueOf(allLinesStake), 2, RoundingMode.HALF_UP);
            return step2.multiply(timesScaleBigDecimal).longValue();
        }else {
            if(times < 1){
                return times;
            }
            return times * timesScale;
        }
    }
}
