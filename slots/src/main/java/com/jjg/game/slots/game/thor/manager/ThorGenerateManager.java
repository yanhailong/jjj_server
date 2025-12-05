package com.jjg.game.slots.game.thor.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressAwardLineInfo;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import com.jjg.game.slots.game.thor.ThorConstant;
import com.jjg.game.slots.game.thor.data.ThorAwardLineInfo;
import com.jjg.game.slots.game.thor.data.ThorResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        log.debug("检查全局分散");

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

            lib.setJackpotId(cfg.getJackpotID());
            break;
        }
        return null;
    }

    @Override
    public void calTimes(ThorResultLib lib) throws Exception {
        if(!checkElement(lib)){
            log.warn("lib = {}", JSONObject.toJSONString(lib));
            throw new IllegalArgumentException("检查结果有错误");
        }


        super.calTimes(lib);
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
//        if (lib.getLibTypeSet().contains(ThorConstant.SpecialMode.FREE)
//                && !checkTriggerFree(lib)) {
//            log.warn("检查免费触发局失败");
//            return false;
//        }
//
//        //检查黄金列车
//        if (lib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_GOLD_TRAIN) && !checkGoldTrainIcon(lib.getIconArr())) {
//            log.warn("检查黄金列车失败");
//            return false;
//        }
//
//        //检查二选一
//        if (lib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_ALL_BOARD) && !checkAllBoard(lib.getIconArr())) {
//            log.warn("检查二选一失败");
//            return false;
//        }
//
//        //检查保险箱
//        if (lib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_SAFE_BOX) && !checkSafeBox(lib)) {
//            log.warn("检查保险箱失败");
//            return false;
//        }
//
//        //检查免费模式
//        if (lib.getLibTypeSet().contains(DollarExpressConstant.SpecialMode.TYPE_TRIGGER_FREE) && !checkFreeIcon(lib.getIconArr())) {
//            log.warn("检查免费模式失败");
//            return false;
//        }
        return true;
    }

    /**
     * 检查免费触发局
     * @param lib
     * @return
     */
    private boolean checkTriggerFree(ThorResultLib lib){
        return true;
    }
}
