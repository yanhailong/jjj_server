package com.jjg.game.slots.game.hulk.manager;

import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
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
    //第三轴扩列成大wild
    private final int TYPE_CHANGE_TO_ONE_WILD = 303101;
    //在二三四扩列成大wild
    private final int TYPE_CHANGE_TO_THREE_WILD = 303102;

    //免费游戏-免费旋转
    private final int TYPE_FREE = 303103;
    //小游戏
    private final int TYPE_MINI_GAME = 303104;


    public HulkGenerateManager() {
        super(HulkResultLib.class);
    }

    @Override
    protected HulkAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        HulkAwardLineInfo awardLineInfo = new HulkAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);
        return awardLineInfo;
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
        super.calTimes(lib);
    }
}
