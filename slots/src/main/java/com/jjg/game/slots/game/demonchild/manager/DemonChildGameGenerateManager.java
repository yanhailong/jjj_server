package com.jjg.game.slots.game.demonchild.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
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
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);
        calTimes(lib);
        return lib;
    }

    @Override
    protected DemonChildAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        DemonChildAwardLineInfo awardLineInfo = new DemonChildAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);
        return awardLineInfo;
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
