package com.jjg.game.slots.game.pegasusunbridle.manager;

import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleAwardLineInfo;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class PegasusUnbridleGameGenerateManager extends AbstractSlotsGenerateManager<PegasusUnbridleAwardLineInfo, PegasusUnbridleResultLib> {
    public PegasusUnbridleGameGenerateManager() {
        super(PegasusUnbridleResultLib.class);
    }

    private int needTimes = 1;
    private int addTimes = 2;

    @Override
    public PegasusUnbridleResultLib checkAward(int[] arr, PegasusUnbridleResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查连线
        List<PegasusUnbridleAwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);
        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);
        calTimes(lib);
        return lib;
    }



    @Override
    protected PegasusUnbridleAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg) {
        PegasusUnbridleAwardLineInfo info = new PegasusUnbridleAwardLineInfo();
        info.setSameIconSet(sameIconIndexSet);
        info.setSameIcon(cfg.getElementId().getFirst());
        info.setBaseTimes(cfg.getBet());
        return info;
    }




    @Override
    public void calTimes(PegasusUnbridleResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<PegasusUnbridleAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (PegasusUnbridleAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }



    @Override
    protected void specialPlayConfig() {
//        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(PegasusUnbridleConstant.SpecialPlay.FREE_GAME_CONFIG_ID);
//        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
//            return;
//        }
//        String[] split = StringUtils.split(specialPlayCfg.getValue());
//        if (split.length != 2) {
//            return;
//        }
//        this.needTimes = Integer.parseInt(split[0]);
//        this.addTimes = Integer.parseInt(split[1]);
    }

    public int getNeedTimes() {
        return needTimes;
    }

    public int getAddTimes() {
        return addTimes;
    }
}
