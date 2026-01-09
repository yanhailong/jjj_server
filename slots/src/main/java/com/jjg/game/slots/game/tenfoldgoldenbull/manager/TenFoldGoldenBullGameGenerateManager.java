package com.jjg.game.slots.game.tenfoldgoldenbull.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.GirdUpdatePropConfig;
import com.jjg.game.slots.data.PropInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.constant.TenFoldGoldenBullConstant;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullAwardLineInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class TenFoldGoldenBullGameGenerateManager extends AbstractSlotsGenerateManager<TenFoldGoldenBullAwardLineInfo, TenFoldGoldenBullResultLib> {
    public TenFoldGoldenBullGameGenerateManager() {
        super(TenFoldGoldenBullResultLib.class);
    }

    private Pair<Integer, Integer> modelRandom;

    @Override
    public TenFoldGoldenBullResultLib checkAward(int[] arr, TenFoldGoldenBullResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查连线
        List<TenFoldGoldenBullAwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);
        //福牛模式转到中奖为止
        if (CollectionUtil.isEmpty(awardLineInfoList) && CollectionUtil.isNotEmpty(lib.getLibTypeSet())) {
            //最大100次
            for (Integer libType : lib.getLibTypeSet()) {
                if (libType != TenFoldGoldenBullConstant.SpecialMode.REAL_LUCKY_BULL) {
                    continue;
                }
                for (int i = 0; i < 100; i++) {
                    TenFoldGoldenBullResultLib tempLib = generateOne(libType);
                    lib.getRandomResult().add(tempLib);
                    if (CollectionUtil.isNotEmpty(tempLib.getAwardLineInfoList())) {
                        break;
                    }
                }
            }
        }
        calTimes(lib);
        return lib;
    }

    @Override
    protected TenFoldGoldenBullAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        TenFoldGoldenBullAwardLineInfo awardLineInfo = new TenFoldGoldenBullAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);
        return awardLineInfo;
    }

    public Pair<Integer, Integer> getModelRandom() {
        return modelRandom;
    }


    @Override
    public void calTimes(TenFoldGoldenBullResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<TenFoldGoldenBullAwardLineInfo> list) {
        if (CollectionUtil.isEmpty(list)) {
            return 0;
        }
        int times = 0;
        for (TenFoldGoldenBullAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(TenFoldGoldenBullConstant.Common.SPECIAL_PLAY_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] modeArr = specialPlayCfg.getValue().split(",");
        if (modeArr.length != 2) {
            return;
        }
        modelRandom = Pair.newPair(Integer.parseInt(modeArr[0]), Integer.parseInt(modeArr[1]));
    }
}
