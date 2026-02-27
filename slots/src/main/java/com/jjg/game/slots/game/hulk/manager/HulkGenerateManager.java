package com.jjg.game.slots.game.hulk.manager;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    protected HulkAwardLineInfo getAwardLineInfo() {
        return new HulkAwardLineInfo();
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
            }


            //是否有jackpot
            if (cfg.getJackpotID() > 0) {
                lib.addJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    public void calTimes(HulkResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //小游戏
        lib.addTimes(miniGame(lib.getSpecialAuxiliaryInfoList()));
        //免费
        lib.addTimes(calFree(lib));
    }

    /**
     * 单线倍数
     * @param awardLineInfoList
     * @return
     */
    private long calLineTimes(List<HulkAwardLineInfo> awardLineInfoList) {
        if(awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return 0;
        }

        long times = 0;
        for(HulkAwardLineInfo info : awardLineInfoList) {
            times += info.getBaseTimes();
        }
        return times;
    }

    private long miniGame(List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList){
        if(specialAuxiliaryInfoList == null || specialAuxiliaryInfoList.isEmpty()) {
            return 0;
        }

        long times = 0;
        for(SpecialAuxiliaryInfo info : specialAuxiliaryInfoList){
            SpecialAuxiliaryCfg cfg = GameDataManager.getSpecialAuxiliaryCfg(info.getCfgId());
            if(cfg.getType() != HulkConstant.SpecialAuxiliary.MINI_GAME){
                continue;
            }

            if(info.getAwardInfos() == null || info.getAwardInfos().isEmpty()){
                continue;
            }

            for(SpecialAuxiliaryAwardInfo awardInfo : info.getAwardInfos()){
                if(awardInfo.getAwardCList() == null || awardInfo.getAwardCList().isEmpty()){
                    continue;
                }

                for(int i : awardInfo.getAwardCList()){
                    times += i;
                }
            }
        }
        return times;
    }
}
