package com.jjg.game.slots.game.demonchild.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.demonchild.data.DemonChildAwardLineInfo;
import com.jjg.game.slots.game.demonchild.data.DemonChildResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class DemonChildGameGenerateManager extends AbstractSlotsGenerateManager<DemonChildAwardLineInfo, DemonChildResultLib> {
    public DemonChildGameGenerateManager() {
        super(DemonChildResultLib.class);
    }


    @Override
    public DemonChildResultLib checkAward(int[] arr, DemonChildResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查中奖线
        List<DemonChildAwardLineInfo> winLines = winLines(lib);
        lib.addAllAwardLineInfo(winLines);
        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);
        calTimes(lib);
        return lib;
    }

    /**
     * 检查免费旋转
     *
     */
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
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

            DemonChildResultLib freeLib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            List<SpecialAuxiliaryInfo> auxiliaryInfos = freeLib.getSpecialAuxiliaryInfoList();
            if (CollectionUtil.isNotEmpty(auxiliaryInfos)) {
                List<JSONObject> freeLibList = new ArrayList<>();
                specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(freeLib));
                for (int j = auxiliaryInfos.size() - 1; j >= 0; j--) {
                    SpecialAuxiliaryInfo auxiliaryInfo = auxiliaryInfos.get(j);
                    if (CollectionUtil.isNotEmpty(auxiliaryInfo.getFreeGames())) {
                        freeLibList.addAll(auxiliaryInfo.getFreeGames());
                    }
                    auxiliaryInfos.remove(j);
                }
                freeLib.setAddFreeCount(freeLibList.size());
                specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(freeLib));
                specialAuxiliaryInfo.getFreeGames().addAll(freeLibList);

            } else {
                specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(freeLib));
            }
        }
    }


    @Override
    public void calTimes(DemonChildResultLib lib) throws Exception {
        //计算奖金倍数
        if (CollectionUtil.isNotEmpty(lib.getSpecialGirdInfoList())) {
            for (SpecialGirdInfo girdInfo : lib.getSpecialGirdInfoList()) {
                if (CollectionUtil.isNotEmpty(girdInfo.getValueMap())) {
                    for (Map.Entry<Integer, Integer> entry : girdInfo.getValueMap().entrySet()) {
                        lib.addTimes(entry.getValue());
                    }
                }
            }
        }
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
    }

    /**
     * 计算中奖线的倍数
     *
     */
    public int calLineTimes(List<DemonChildAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int times = 0;
        for (DemonChildAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    /**
     * 计算免费游戏的总倍数
     */
    public long calFree(DemonChildResultLib lib, int endIndex) {
        if (CollectionUtil.isEmpty(lib.getSpecialAuxiliaryInfoList())) {
            return 0;
        }
        long totalTimes = 0;
        for (SpecialAuxiliaryInfo info : lib.getSpecialAuxiliaryInfoList()) {
            if (CollectionUtil.isEmpty(info.getFreeGames())) {
                continue;
            }
            endIndex = Math.min(endIndex, info.getFreeGames().size());
            for (int i = 0; i < endIndex; i++) {
                JSONObject jsonObject = info.getFreeGames().get(i);
                DemonChildResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), DemonChildResultLib.class);
                //中奖线
                totalTimes += tmpLib.getTimes();
            }
        }
        return totalTimes;
    }


}
