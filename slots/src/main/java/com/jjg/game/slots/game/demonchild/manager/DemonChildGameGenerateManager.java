package com.jjg.game.slots.game.demonchild.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.demonchild.constant.DemonChildConstant;
import com.jjg.game.slots.game.demonchild.data.DemonChildAwardLineInfo;
import com.jjg.game.slots.game.demonchild.data.DemonChildResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

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
        //如果中间不是wild图标去除奖池
        int[] iconArr = lib.getIconArr();
        if (CollectionUtil.isNotEmpty(lib.getJackpotIds()) &&
                iconArr.length <= DemonChildConstant.Common.MIND_INDEX || iconArr[DemonChildConstant.Common.MIND_INDEX] != DemonChildConstant.BaseElement.GOLD_GET) {
            lib.getJackpotIds().clear();
        }
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);
        //设置免费总次数
        if (CollectionUtil.isNotEmpty(overallDisperseAuxiliaryInfoList)) {
            for (SpecialAuxiliaryInfo auxiliaryInfo : overallDisperseAuxiliaryInfoList) {
                if (CollectionUtil.isNotEmpty(auxiliaryInfo.getFreeGames())) {
                    lib.setFreeTotalCount(lib.getFreeTotalCount() + auxiliaryInfo.getFreeGames().size());
                }
            }
        }
        calTimes(lib);
        return lib;
    }


    @Override
    protected DemonChildAwardLineInfo getAwardLineInfo() {
        return new DemonChildAwardLineInfo();
    }

    @Override
    public void calTimes(DemonChildResultLib lib) throws Exception {
        //计算奖金倍数
        if (CollectionUtil.isNotEmpty(lib.getSpecialGirdInfoList())) {
            for (SpecialGirdInfo girdInfo : lib.getSpecialGirdInfoList()) {
                if (CollectionUtil.isNotEmpty(girdInfo.getValueMap())) {
                    //计算网格中奖的
                    int[] iconArr = lib.getIconArr();
                    if (iconArr.length <= DemonChildConstant.Common.MIND_INDEX || iconArr[DemonChildConstant.Common.MIND_INDEX] != DemonChildConstant.BaseElement.GOLD_GET) {
                        continue;
                    }
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

}
