package com.jjg.game.slots.game.hulk.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialModeCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.hulk.data.HulkAwardLineInfo;
import com.jjg.game.slots.game.hulk.data.HulkResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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
    public SpecialAuxiliaryInfo triggerMiniGame(int specialModeType, int[] arr, int miniGameId, List<SpecialGirdInfo> specialGirdInfoList) {
        log.debug("触发小游戏 miniGameId = {}", miniGameId);
        //根据小游戏id去找相关配置
        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
        if (specialAuxiliaryCfg == null) {
            log.warn("未找到该小游戏的配置 miniGameId = {}", miniGameId);
            return null;
        }

        SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(miniGameId);
        if (specialAuxiliaryPropConfig == null) {
            log.warn("未找到该小游戏小关的权重信息配置 miniGameId = {}", miniGameId);
            return null;
        }

        SpecialAuxiliaryInfo specialAuxiliaryInfo = new SpecialAuxiliaryInfo();
        specialAuxiliaryInfo.setCfgId(miniGameId);
        //检查免费旋转
        triggerFree(specialModeType, arr, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo);
        //检查是否有额外奖励
        triggerAuxiliaryExtra(arr, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
        return specialAuxiliaryInfo;
    }

    protected void triggerFree(int specialModeType, int[] arr, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        for (int i = 0; i < freeCount; i++) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            HulkResultLib lib = generateFreeOne(specialModeType, arr, specialAuxiliaryCfg, specialGroupGirdID);
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
        }
    }

    public HulkResultLib generateFreeOne(int specialModeType, int[] beforeArr, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }

            //创建结果库对象
            HulkResultLib lib = createResultLib();
            lib.setId(RandomUtils.getUUid());
            lib.setRollerMode(specialModeCfg.getRollerMode());

            //获取rollerMode
            int rollerMode = specialAuxiliaryCfg.getRollerMode();
            if (rollerMode < 1) {
                rollerMode = specialModeCfg.getRollerMode();
            }

            int[] arr;
            if (specialAuxiliaryCfg.getType() == TYPE_CHANGE_TO_ONE_WILD) {
                arr = new int[beforeArr.length];
                System.arraycopy(beforeArr, 0, arr, 0, beforeArr.length);
            } else if (specialAuxiliaryCfg.getType() == TYPE_CHANGE_TO_THREE_WILD) {
                arr = new int[beforeArr.length];
                System.arraycopy(beforeArr, 0, arr, 0, beforeArr.length);
            } else {
                //生成所有的图标
                arr = generateAllIcons(rollerMode, specialModeCfg.getCols(), specialModeCfg.getRows());
            }

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
            //判断中奖，返回
            return checkAward(arr, lib, true);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
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
    public void calTimes(HulkResultLib lib) throws Exception {
        super.calTimes(lib);
    }
}
